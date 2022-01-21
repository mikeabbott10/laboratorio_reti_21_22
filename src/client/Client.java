package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import client.http.HttpRequests;
import client.multicast.MulticastNotificationReceiver;
import client.rmi.ClientNotifyEventImplementation;
import client.rmi.ClientNotifyEventInterface;
import client.social.Post;
import client.social.PostComment;
import client.social.User;
import client.social.WalletTransaction;
import client.util.ClientConfig;
import client.util.JacksonUtil;
import exceptions.AlreadyConnectedException;
import exceptions.DatabaseException;
import exceptions.InvalidUsername;
import exceptions.LoginException;
import exceptions.TooManyTagsException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import server.rmi.ServerRMIInterface;

public class Client {
    public ClientConfig client_config; // get it from file

    public final int BUF_SIZE = 4096;

    protected ServerRMIInterface serverRMIObj;
    protected ClientNotifyEventInterface stub;
    protected ClientNotifyEventInterface callbackObj;

    protected Thread multicastReceiverThread = null;

    protected String username = null;
    protected String password = null;
    public Set<String> followers = new HashSet<>();
    public AtomicBoolean logoutNotification;

    protected BufferedReader consoleReader;
    public boolean printMulticastNotification;
    private boolean testEnvironment;
    private String[] commands;

    Client(ClientConfig client_config){
        this.client_config = client_config;
        this.logoutNotification = new AtomicBoolean(false);
        this.printMulticastNotification = true;
        this.testEnvironment = false;
        this.commands = new String[]{""};
    }

    Client(ClientConfig client_config, String commandsString){
        this.client_config = client_config;
        this.logoutNotification = new AtomicBoolean(false);
        this.printMulticastNotification = true;
        this.testEnvironment = true;
        this.commands = commandsString.split("\\|\\s*");
    }

    public void start(){
        // RMI
        try{
            // Looking for server
            serverRMIObj = 
                (ServerRMIInterface) Naming.lookup(client_config.RMIServerUrl + client_config.rmiServiceName);

            // prepare callback object
            callbackObj = new ClientNotifyEventImplementation(this);
            stub = (ClientNotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);
        }catch(Exception e){
            System.out.println(e.getMessage());
            return;
        }
        
        consoleReader = new BufferedReader(new InputStreamReader(System.in));

        int commandsIndex = -1;

        String cmd;
        while (!ClientMain.quit && commandsIndex < commands.length - 1) {
            commandsIndex = testEnvironment ? commandsIndex+1 : -1;
            try {
                System.out.printf("> ");
                cmd = testEnvironment ? commands[commandsIndex] : consoleReader.readLine().trim();
                System.out.println(cmd);
            } catch (IOException e1) {
                e1.printStackTrace();
                break;
            }

            try{
                if(logoutNotification.get())
                    autoLogout();
                doWork(cmd);
            }catch(PatternSyntaxException e){
                System.out.println("Comando non valido.");
                e.printStackTrace();
                printUsage();
            }
        }

        if(testEnvironment)
            doWork("quit");

        if(multicastReceiverThread == null)
            return;
        multicastReceiverThread.interrupt();
        System.out.println("Wait...");
        try {
            multicastReceiverThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Bye!");
    }

    private void doWork(String cmd) {
        // \\s+ almeno uno spazio, \\s* 0 o più spazi, .* 0 o più caratteri qualsiasi, .+ 1 o più caratteri qualsiasi
        // quit client
        if (Pattern.matches("^quit\\s*$", cmd)) {
            ClientMain.quit = true;
            if(username!=null)
                sendLogoutRequest();
            return;
        }

        // enable/disable print reward received notification
        if (Pattern.matches("^reward\\s+notification\\s*$", cmd)) {
            printMulticastNotification = !printMulticastNotification;
            System.out.println(printMulticastNotification?"Enabled.":"Disabled."); 
            return;
        }

        // register
        if (Pattern.matches("^register\\s+\\S+\\s+\\S+\\s+.*\\s*$", cmd)) { 
            if (username != null) {
                System.out.println("Please logout."); 
                return;
            }
            String[] param = cmd.split("\\s+", 4);
            //System.out.println("param: "+param);
            String[] tags = null;
            if( param.length == 4 ){
                tags = param[3].split(" ");
            }
            //System.out.println("tags: "+tags);
            try {
                System.out.println(serverRMIObj.register(param[1], param[2], tags));
            } catch (RemoteException | DatabaseException e) {
                e.printStackTrace();
            }catch(TooManyTagsException | InvalidUsername ex){
                System.out.println(ex.getMessage());
            }
            return;
        }

        // login and connect to multicast group and rmi callback
        if (Pattern.matches("^login\\s+\\S+\\s+\\S+\\s*$", cmd)) {
            if(username!=null){
                System.out.println("An user is already logged in."); 
                return;
            }
            String u = new String(cmd.split("\\s+")[1]);
            String p = new String(cmd.split("\\s+")[2]);
            //System.out.println(u + "-"+ p);
            setUsernameAndPassword(u, p);

            //rmi
            try {
                serverRMIObj.registerForCallback(stub, username, password);
            } catch (RemoteException | DatabaseException e) {
                System.out.println(e.getMessage());
                setUsernameAndPassword(null, null);
                return;
            }catch(AlreadyConnectedException e1){
                System.out.println(e1.getMessage());
                setUsernameAndPassword(null, null);
                return;
            }catch(LoginException ex){
                setUsernameAndPassword(null, null);
                System.out.println("Invalid username or password.");
                return;
            }

            // send http req
            String response = sendLoginRequest();
            if( response==null ){
                setUsernameAndPassword(null, null);
                return;
            }

            // MULTICAST
            try{
                MulticastGroupData multicast_data = 
                    (MulticastGroupData) JacksonUtil.getObjectFromString(response, MulticastGroupData.class);
                InetSocketAddress group = 
                    new InetSocketAddress(
                        InetAddress.getByName(multicast_data.getMulticast_group_ip()), 
                        Integer.parseInt(multicast_data.getMulticast_group_port())
                    );
                NetworkInterface netInt = NetworkInterface.getByName("enp42s0");
                
                // start notification receiver
                multicastReceiverThread = new Thread(
                    new MulticastNotificationReceiver(this, Integer.parseInt(multicast_data.getMulticast_group_port()), group, netInt));
                multicastReceiverThread.start();

                System.out.println("User "+ username + " logged in");
            }catch(JsonProcessingException e){
                //e.printStackTrace();
                ServerResponse serverResponse;
                try {
                    serverResponse = (ServerResponse) JacksonUtil.getObjectFromString(response, ServerResponse.class);
                } catch (JsonProcessingException e1) {
                    e1.printStackTrace();
                    return;
                }
                System.out.println(serverResponse.getMessage());
            }catch(IOException e1){
                System.out.println("Cannot join multicast group");
                e1.printStackTrace();
                multicastReceiverThread.interrupt();
                System.out.println("Wait...");
                try {
                    multicastReceiverThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Done...");
            }
            return;
        }

        // logout
        if (Pattern.matches("^logout\\s*$", cmd)) {
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }

            // send http req
            String response = sendLogoutRequest();
            if( response==null ){
                setUsernameAndPassword(null, null);
                return;
            }

            // stop reward notification receiver
            multicastReceiverThread.interrupt();
            System.out.println("Wait...");
            try {
                multicastReceiverThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Done.");
            // Unregistering for callback
            try {
                serverRMIObj.unregisterForCallback(stub);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            setUsernameAndPassword(null,null);
            System.out.println("Logged out");
            return;
        }

        /**
         * Comando: list users
         * utilizzata da un utente per visualizzare la lista (parziale) degli utenti registrati al servizio. Il server
         * restituisce la lista di utenti che hanno almeno un tag in comune con l’utente che ha fatto la richiesta.
         */
        if (Pattern.matches("^list\\s+users$", cmd)) {
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }
            String response = listUsersRequest();
            if(response != null){
                ListUsersData sr;
                try {
                    sr = (ListUsersData) JacksonUtil.getObjectFromString(response, ListUsersData.class);
                    for (String tag : sr.users.keySet()) {
                        System.out.println("Tag: "+ tag);
                        for (User user : sr.users.get(tag)) {
                            if(!user.getUsername().equals(username))
                                System.out.println(user.getUsername());
                        }
                        System.out.println("");
                    }
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    ServerResponse serverResponse;
                    try {
                        serverResponse = (ServerResponse) JacksonUtil.getObjectFromString(response, ServerResponse.class);
                    } catch (JsonProcessingException e1) {
                        e1.printStackTrace();
                        return;
                    }
                    System.out.println(serverResponse.getMessage());
                }
            }
            return;
        }

        /**
         * Comando: list followers
         * operazione lato client per visualizzare la lista dei propri follower. Questo comando
         * dell’utente non scatena una richiesta sincrona dal client al server. Il client restituisce la lista dei follower
         * mantenuta localmente che viene via via aggiornata grazie a notifiche “asincrone” ricevute dal server.
         */
        if (Pattern.matches("^list\\s+followers$", cmd)) {
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }
            System.out.println(username + "followers: ");
            for (String name : followers) {
                System.out.println(name);
            }
            return;
        }

        /**
         * Comando: list following
         * utilizzata da un utente per visualizzare la lista degli utenti di cui è follower.
         */
        if (Pattern.matches("^list\\s+following$", cmd)) {
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }
            String response = getUserRequest(username);
            if(response != null){
                UserIdData sr;
                try {
                    sr = (UserIdData) JacksonUtil.getObjectFromString(response, UserIdData.class);
                    System.out.println(username + " is following: ");
                    for (String name : sr.user.getFollowing()) {
                        System.out.println(name);
                    }
                } catch (JsonProcessingException e) {
                    ServerResponse serverResponse;
                    try {
                        serverResponse = (ServerResponse) JacksonUtil.getObjectFromString(response, ServerResponse.class);
                    } catch (JsonProcessingException e1) {
                        e1.printStackTrace();
                        return;
                    }
                    System.out.println(serverResponse.getMessage());
                }
            }
            return;
        }

        /**
         * Comando: unfollow <username>
         * l’utente chiede di non seguire più l’utente che ha per username idUser.
         */
        if (Pattern.matches("^unfollow\\s+\\S+$", cmd)) {
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }
            String name = new String(cmd.split("\\s+")[1]);
            String response = sendUnfollowRequest(name);
            if(response!=null){
                try {
                    ServerResponse sr = (ServerResponse) JacksonUtil.getObjectFromString(response, ServerResponse.class);
                    System.out.println(sr.message);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
            return;
        }

        /**
         * Comando: follow <username>
         * l’utente chiede di seguire l’utente che ha per username idUser. Da quel momento in poi
         * può ricevere tutti i post pubblicati da idUser.
         */
        if (Pattern.matches("^follow\\s+\\S+$", cmd)) {
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }
            String name = new String(cmd.split("\\s+")[1]);
            String response = sendFollowRequest(name);
            if(response!=null){
                try {
                    ServerResponse sr = (ServerResponse) JacksonUtil.getObjectFromString(response, ServerResponse.class);
                    System.out.println(sr.message);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
            return;
        }

        /**
         * Comando: blog
         * operazione per recuperare la lista dei post di cui l’utente è autore. Viene restituita una lista dei
         * post presenti nel blog dell’utente. Per ogni post viene fornito id del post, autore e titolo.
         */
        if (Pattern.matches("^blog$", cmd)) {
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }
            String response = getUserPostsRequest(username);
            if(response!=null){
                UserBlogData sr;
                try {
                    sr = (UserBlogData) JacksonUtil.getObjectFromString(response, UserBlogData.class);
                    for (Post post : sr.posts) {
                        System.out.printf("Post id: " + post.getId());
                        System.out.printf("\tAuthor: " + post.getAuthor());
                        System.out.printf("\tTitle: " + post.getTitle() + "\n");
                    }
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    ServerResponse serverResponse;
                    try {
                        serverResponse = (ServerResponse) JacksonUtil.getObjectFromString(response, ServerResponse.class);
                    } catch (JsonProcessingException e1) {
                        e1.printStackTrace();
                        return;
                    }
                    System.out.println(serverResponse.getMessage());
                }
            }
            return;
        }

        /**
         * Comando: post \n <title> \n <content>
         * operazione per pubblicare un nuovo post. L’utente deve fornire titolo e
         * contenuto del post. Il titolo ha lunghezza massima di 20 caratteri e il contenuto una lunghezza massima di
         * 500 caratteri. Se l’operazione va a buon fine, il post è creato e disponibile per i follower dell’autore del post.
         * Il sistema assegna un identificatore univoco a ciascun post creato (idPost).
         */
        if (Pattern.matches("^post\\s+\".+\"\\s+\".+\"\\s*$", cmd)) {
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }

            String[] param = cmd.split("\"");
            final String title = new String(param[1]);
            final String content = new String(param[3]);
            if(content.length()>500){
                System.out.println("Content max length: 500 characters. Operation rejected.");
                return;
            }
            if(title.length()>20){
                System.out.println("Title max length: 20 characters. Operation rejected.");
                return;
            }
            
            HashMap<String, String> post = new HashMap<String, String>() {{
                put("title", title);
                put("content", content);
            }};;
            var postObjectMapper = new ObjectMapper();
            String createPostRequestBody;
            try {
                createPostRequestBody = postObjectMapper.writeValueAsString(post);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return;
            }

            String response = createPostRequest(createPostRequestBody);
            if(response != null){
                PostIdData sr;
                try {
                    sr = (PostIdData) JacksonUtil.getObjectFromString(response, PostIdData.class);
                    System.out.println("Post created, postId: "+sr.postID);
                } catch (Exception e) {
                    ServerResponse serverResponse;
                    try {
                        serverResponse = (ServerResponse) JacksonUtil.getObjectFromString(response, ServerResponse.class);
                    } catch (JsonProcessingException e1) {
                        e1.printStackTrace();
                        return;
                    }
                    System.out.println(serverResponse.getMessage());
                }
            }
            return;
        }

        /**
         * Comando: show feed
         * operazione per recuperare la lista dei post nel proprio feed. Viene restituita una lista dei post.
         * Per ogni post viene fornito id, autore e titolo del post.
         */
        if (Pattern.matches("^show\\s+feed$", cmd)){
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }
            String response = getUserFeedRequest();
            if(response!=null){
                UserFeedData sr;
                try {
                    sr = (UserFeedData) JacksonUtil.getObjectFromString(response, UserFeedData.class);
                    System.out.println("Feed:\n");
                    for (String name : sr.feed.keySet()) {
                        for (Post post : sr.feed.get(name)) {
                            System.out.println("Post id: " + post.getId());
                            System.out.println("Author: " + post.getAuthor());
                            System.out.println("Title: " + post.getTitle() + "\n");
                        }
                    } 
                } catch (Exception e) {
                    ServerResponse serverResponse;
                    try {
                        serverResponse = (ServerResponse) JacksonUtil.getObjectFromString(response, ServerResponse.class);
                    } catch (JsonProcessingException e1) {
                        e1.printStackTrace();
                        return;
                    }
                    System.out.println(serverResponse.getMessage());
                }
            }
            return;
        }

        /**
         * Comando: show post <idPost>
         * il server restituisce titolo, contenuto, numero di voti positivi, numero di voti negativi e
         * commenti del post. Se l’utente è autore del post può cancellare il post con tutto il suo contenuto associato
         * (commenti e voti). Se l’utente ha il post nel proprio feed può esprimere un voto, positivo o negativo (solo un
         * voto, successivi tentativi di voto non saranno accettati dal server, che restituirà un messaggio di errore) e/o
         * inserire un commento.
         */
        if (Pattern.matches("^show\\s+post\\s+\\S+$", cmd)){
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }
            String postID = new String(cmd.split("\\s+")[2]);
            int postId;
            try{
                postId = Integer.parseInt(postID);
            }catch(NumberFormatException e){
                System.out.println("Comando non valido.");
                printUsage();
                return;
            }
            String response = getPostFromIdRequest(postId);
            if(response!=null){
                PostData sr;
                try {
                    sr = (PostData) JacksonUtil.getObjectFromString(response, PostData.class);
                    System.out.println("Title: " + sr.post.getTitle());
                    System.out.println("Content: " + sr.post.getContent());
                    System.out.println("Upvotes: " + sr.post.getUpvotes());
                    System.out.println("Downvotes: " + sr.post.getDownvotes());
                    System.out.println("Comments:");
                    for (PostComment comment : sr.post.getComments()) {
                        System.out.println("\tAuthor: "+ comment.getAuthor() + " - Date: " + comment.getDate());
                        System.out.println("\t"+comment.getComment());
                    }
                } catch (JsonProcessingException e) {
                    ServerResponse serverResponse;
                    try {
                        serverResponse = (ServerResponse) JacksonUtil.getObjectFromString(response, ServerResponse.class);
                    } catch (JsonProcessingException e1) {
                        e1.printStackTrace();
                        return;
                    }
                    System.out.println(serverResponse.getMessage());
                    return;
                }
                if(sr.post.getAuthor().equals(username)){
                    //user is the post author
                    System.out.println("Do you want to delete this post? y/n");
                    try {
                        System.out.printf("> ");
                        String deleteThis = consoleReader.readLine().trim();
                        if(deleteThis.toLowerCase().equals("y")){
                            //delete post
                            String deleteResponse = sendDeleteRequest(sr.post.getId());
                            if(deleteResponse!=null){
                                ServerResponse serverResponse;
                                try {
                                    serverResponse = (ServerResponse) JacksonUtil.getObjectFromString(deleteResponse, ServerResponse.class);
                                } catch (JsonProcessingException e1) {
                                    e1.printStackTrace();
                                    return;
                                }
                                System.out.println(serverResponse.getMessage());
                                return;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }else{
                    //user is not the post author
                    System.out.println("Do you want to rate this post? (Only followers of the author can do it) y/n");
                    try {
                        System.out.printf("> ");
                        String voteThis = consoleReader.readLine().trim();
                        if(voteThis.toLowerCase().equals("y")){
                            System.out.println("0/downvote, 1/upvote: ");
                            System.out.printf("> ");
                            voteThis = consoleReader.readLine().trim();
                            int vote;
                            try{
                                vote = Integer.parseInt(voteThis);
                                if(vote!=1 && vote!=0){
                                    System.out.println("Comando non valido.");
                                    printUsage();
                                    return;
                                }
                            }catch(NumberFormatException e){
                                System.out.println("Comando non valido.");
                                printUsage();
                                return;
                            }
                            //rate post
                            String rateResponse = sendRateRequest(postId, vote==1);
                            if(response!=null){
                                ServerResponse serverResponse;
                                try {
                                    serverResponse = (ServerResponse) JacksonUtil.getObjectFromString(rateResponse, ServerResponse.class);
                                } catch (JsonProcessingException e1) {
                                    e1.printStackTrace();
                                    return;
                                }
                                System.out.println(serverResponse.getMessage());
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
            return;
        }

        /**
         * Comando: delete <idPost>
         * operazione per cancellare un post. La richiesta viene accettata ed eseguita solo se
         * l’utente è l’autore del post. Il server cancella il post con tutto il suo contenuto associato (commenti e voti).
         * Non vengono calcolate ricompense “parziali”, ovvero se un contenuto recente (post, voto o commento) non
         * era stato conteggiato nel calcolo delle ricompense perché ancora il periodo non era scaduto, non viene
         * considerato nel calcolo delle ricompense.
         */
        if (Pattern.matches("^delete\\s+\\S+$", cmd)){
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }
            String postID = new String(cmd.split("\\s+")[1]);
            int postId;
            try{
                postId = Integer.parseInt(postID);
            }catch(NumberFormatException e){
                System.out.println("Comando non valido.");
                printUsage();
                return;
            }
            String response = sendDeleteRequest(postId);
            if(response!=null){
                ServerResponse serverResponse;
                try {
                    serverResponse = (ServerResponse) JacksonUtil.getObjectFromString(response, ServerResponse.class);
                } catch (JsonProcessingException e1) {
                    e1.printStackTrace();
                    return;
                }
                System.out.println(serverResponse.getMessage());
            }
            return;
        }

        /**
         * Comando: rewin <idPost>
         * operazione per effettuare il rewin di un post, ovvero per pubblicare nel proprio blog un
         * post presente nel proprio feed.
         */
        if (Pattern.matches("^rewin\\s+\\S+$", cmd)){
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }
            String postID = new String(cmd.split("\\s+")[1]);
            int postId;
            try{
                postId = Integer.parseInt(postID);
            }catch(NumberFormatException e){
                System.out.println("Comando non valido.");
                printUsage();
                return;
            }
            String response = sendRewinRequest(postId);
            if(response!=null){
                ServerResponse serverResponse;
                try {
                    serverResponse = (ServerResponse) JacksonUtil.getObjectFromString(response, ServerResponse.class);
                } catch (JsonProcessingException e1) {
                    e1.printStackTrace();
                    return;
                }
                System.out.println(serverResponse.getMessage());
            }
            return;
        }

        /**
         * Comando: rate <idPost> <vote>
         * operazione per assegnare un voto positivo o negativo ad un post. Se l’utente ha il
         * post nel proprio feed e non ha ancora espresso un voto, il voto viene accettato, negli altri casi (ad es. ha già
         * votato il post, non ha il post nel proprio feed, è l’autore del post) il voto non viene accettato e il server
         * restituisce un messaggio di errore. 
         * Nel comando i voti sono così codificati: voto positivo +1, voto negativo -1.
         */
        if (Pattern.matches("^rate\\s+\\S+\\s+\\S+$", cmd)){
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }
            String postID = new String(cmd.split("\\s+")[1]);
            String voteStr = new String(cmd.split("\\s+")[2]);
            int postId, vote;
            try{
                postId = Integer.parseInt(postID);
                vote = Integer.parseInt(voteStr);
                if(vote!=1 && vote!=-1){
                    System.out.println("Comando non valido.");
                    printUsage();
                    return;
                }
            }catch(NumberFormatException e){
                System.out.println("Comando non valido.");
                printUsage();
                return;
            }
            String response = sendRateRequest(postId, vote>0);
            if(response!=null){
                ServerResponse serverResponse;
                try {
                    serverResponse = (ServerResponse) JacksonUtil.getObjectFromString(response, ServerResponse.class);
                } catch (JsonProcessingException e1) {
                    e1.printStackTrace();
                    return;
                }
                System.out.println(serverResponse.getMessage());
            }
            return;
        }

        /**
         * Comando: comment <idPost> <comment>
         * operazione per aggiungere un commento ad un post. Se l’utente ha il post
         * nel proprio feed, il commento viene accettato, negli altri casi (ad es. l’utente non ha il post nel proprio feed
         * oppure è l’autore del post) il commento non viene accettato e il server restituisce un messaggio di errore.
         * Un utente può aggiungere più di un commento ad un post
         */
        if (Pattern.matches("^comment\\s+\\S+\\s+\\S+\\s*$", cmd)){
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }
            //get params
            String[] param = cmd.split("\\s+", 3);
            //System.out.println("param: "+param);
            String commento = null;
            try{
                commento = param[2];
            }catch(ArrayIndexOutOfBoundsException e){
                System.out.println("Comando non valido.");
                printUsage();
                return;
            }
            final String comment = commento;

            String postID = new String(param[1]);
            int postId;
            try{
                postId = Integer.parseInt(postID);
            }catch(NumberFormatException e){
                System.out.println("Comando non valido.");
                printUsage();
                return;
            }
            String commentPostRequestBody;
            try {
                var postComment = new HashMap<String, String>() {{
                    put("comment", comment);
                }};
                var commentObjectMapper = new ObjectMapper();
                
                commentPostRequestBody = commentObjectMapper.writeValueAsString(postComment);

            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return;
            }
            String response = sendCommentRequest(postId, commentPostRequestBody);
            if(response!=null){
                ServerResponse serverResponse;
                try {
                    serverResponse = (ServerResponse) JacksonUtil.getObjectFromString(response, ServerResponse.class);
                } catch (JsonProcessingException e1) {
                    e1.printStackTrace();
                    return;
                }
                System.out.println(serverResponse.getMessage());
            }
            return;
        }

        /**
         * Comando: wallet
         * operazione per recuperare il valore del proprio portafoglio. Il server restituisce il totale e la
         * storia delle transazioni (ad es. <incremento> <timestamp>).
         */
        if (Pattern.matches("^wallet$", cmd)){
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }
            String response = sendWalletRequest(false);
            if(response!=null){
                WalletData sr;
                try {
                    sr = (WalletData) JacksonUtil.getObjectFromString(response, WalletData.class);
                    System.out.println("Your wallet: " + sr.wallet);
                    if(sr.wallet_history.size()==0) 
                        return;
                    System.out.println("Your wallet history:\n");
                    for (WalletTransaction wt  : sr.wallet_history) {
                        System.out.println("Date: " + wt.getDate() + ", Increase: " + wt.getAmount());
                    }
                } catch (JsonProcessingException e) {
                    ServerResponse serverResponse;
                    try {
                        serverResponse = (ServerResponse) JacksonUtil.getObjectFromString(response, ServerResponse.class);
                    } catch (JsonProcessingException e1) {
                        e1.printStackTrace();
                        return;
                    }
                    System.out.println(serverResponse.getMessage());
                    return;
                }
            }
            return;
        }

        /**
         * Comando: wallet btc
         * operazione per recuperare il valore del proprio portafoglio convertito in bitcoin. Il
         * server utilizza il servizio di generazione di valori random decimali fornito da RANDOM.ORG per ottenere un
         * tasso di cambio casuale e quindi calcola la conversione.
         */
        if (Pattern.matches("^wallet\\s+btc$", cmd)){
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }
            String response = sendWalletRequest(true);
            if(response!=null){
                Wallet sr;
                try {
                    sr = (Wallet) JacksonUtil.getObjectFromString(response, Wallet.class);
                    System.out.println("Your wallet in btc: " + sr.wallet);
                } catch (JsonProcessingException e) {
                    ServerResponse serverResponse;
                    try {
                        serverResponse = (ServerResponse) JacksonUtil.getObjectFromString(response, ServerResponse.class);
                    } catch (JsonProcessingException e1) {
                        e1.printStackTrace();
                        return;
                    }
                    System.out.println(serverResponse.getMessage());
                    return;
                }
            }
            return;
        }

        System.out.println("Comando non valido.");
        printUsage();
    }

    private void printUsage() {
        System.out.println(
            String.join("\n",
                "register <username> <password> <tags>: register a new user",
                "login <username> <password>: login an user",
                "logout: logout the current user",
                "list users: show users with at least one common tag with the current user",
                "list followers: show the followers of the current user",
                "list following: show the users followed by the current user",
                "follow <username>: follow an user",
                "unfollow <username>: unfollow an user",
                "blog: show the posts of the current user",
                "post <title> <content>: create a new post",
                "show feed: show the feed of the current user",
                "show post <postId>: show the post with id <postId>",
                "delete <postId>: delete the post with id <postId>",
                "rewin <postId>: rewin the post with id <postId>",
                "rate <postId> +1/-1: rate the post with id <postId>",
                "comment <postId> <comment>: comment the post with id <postId>",
                "wallet [btc]: show the wallet of the current user [in bitcoin]",
                "reward notification: enable/disable reward update notification",
                "quit: quit"
            )
        );
    }

    private void setUsernameAndPassword(String username1, String password1) {
        username = username1;
        password = password1;
    }
    private void autoLogout(){
        // stop reward notification receiver
        setUsernameAndPassword(null, null);
        multicastReceiverThread.interrupt();
        System.out.println("Wait...");
        try {
            multicastReceiverThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("User logged out!");
    }


    protected String sendLogoutRequest() {
        try {
            return HttpRequests.put("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/logout", null,
                                    username, password);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    private String sendLoginRequest()  {
        try {
            return HttpRequests.put("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/login", null,
                                    username, password);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    private String createPostRequest(String body)  {
        try {
            return HttpRequests.post("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/post", 
                                    body, username, password);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    private String listUsersRequest()  {
        try {
            return HttpRequests.get("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/users", 
                    username, password);
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    private String getUserRequest(String userID)  {
        try {
            return HttpRequests.get("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/user/" + userID, 
                    username, password);
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    private String sendFollowRequest(String userID)  {
        try {
            return HttpRequests.put("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/user/"+ userID +"/action/follow", null,
                    username, password);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private String sendUnfollowRequest(String userID)  {
        try {
            return HttpRequests.put("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/user/"+ userID +"/action/unfollow", null,
                    username, password);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    private String getUserPostsRequest(String userID)  {
        try {
            return HttpRequests.get("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/posts/"+ userID,
                    username, password);
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    private String getUserFeedRequest()  {
        try {
            return HttpRequests.get("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/feed",
                    username, password);
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    private String getPostFromIdRequest(int postID)  {
        try {
            return HttpRequests.get("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/post/"+postID,
                    username, password);
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    private String sendDeleteRequest(int postID)  {
        try {
            return HttpRequests.delete("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/post/"+postID,
                    username, password);
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    private String sendRewinRequest(int postID)  {
        try {
            return HttpRequests.put("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/post/"+postID+"/action/rewin", null,
                    username, password);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    private String sendRateRequest(int postID, boolean upVote)  {
        try {
            return HttpRequests.put("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/post/"+postID+"/action/"+(upVote?"vote":"unvote"), 
                    null, username, password);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    private String sendCommentRequest(int postID, String comment)  {
        try {
            return HttpRequests.put("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/post/"+postID+"/action/comment", 
                    comment, username, password);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    private String sendWalletRequest(boolean btc){
        try {
            return HttpRequests.get("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/user/" + username + "/wallet/"+(btc?"1":"0"),
                                    username, password);
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }


    @NoArgsConstructor
    private static @Data class ServerResponse{
        private String message;
    }

    @NoArgsConstructor
    private static @Data class MulticastGroupData{
        private String multicast_group_ip;
        private String multicast_group_port;


    }

    @NoArgsConstructor
    private static @Data class PostIdData{
        private Integer postID;
    }

    @NoArgsConstructor
    private static @Data class UserIdData{
        private User user;
    }

    @NoArgsConstructor
    private static @Data class ListUsersData{
        private HashMap<String, HashSet<User>> users;
    }

    @NoArgsConstructor
    private static @Data class UserBlogData{
        private Post[] posts;
    }

    @NoArgsConstructor
    private static @Data class UserFeedData{
        private HashMap<String, Post[]> feed;
    }

    @NoArgsConstructor
    private static @Data class PostData{
        private Post post;
    }

    @NoArgsConstructor
    private static @Data class Wallet{
        protected double wallet;
    }

    @NoArgsConstructor
    @EqualsAndHashCode(callSuper=false)
    private static @Data class WalletData extends Wallet{
        private Set<WalletTransaction> wallet_history;
    }

}
