package client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.channels.MulticastChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import client.http.HttpRequests;
import client.multicast.MulticastNotificationReceiver;
import client.rmi.ClientNotifyEventImplementation;
import client.rmi.ClientNotifyEventInterface;
import client.util.ClientConfig;
import client.util.Constants;
import client.util.JacksonUtil;
import exceptions.AlreadyConnectedException;
import exceptions.DatabaseException;
import exceptions.InvalidTags;
import exceptions.InvalidUsername;
import exceptions.LoginException;
import exceptions.TooManyTagsException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import server.rmi.ServerRMIInterface;

public class ClientMain {
    public static ClientConfig client_config; // get it from file

    public static final int BUF_SIZE = 4096;

    private static ServerRMIInterface serverRMIObj;
    private static ClientNotifyEventInterface stub;

    private static Thread multicastReceiverThread;

    public static boolean quit;

    private static String username = null;
    private static String password = null;

    private static BufferedReader consoleReader;

    public static void main(String args[]){
        try {
            client_config = getClientConfig();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // RMI
        try{
            // Looking for server
            serverRMIObj = 
                (ServerRMIInterface) Naming.lookup(client_config.RMIServerUrl + client_config.rmiServiceName);

            // prepare callback object
            ClientNotifyEventInterface callbackObj = new ClientNotifyEventImplementation();
            stub = (ClientNotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);
        }catch(Exception e){
            e.printStackTrace();
            return;
        }
        
        consoleReader = new BufferedReader(new InputStreamReader(System.in));

        String cmd;
        while (!quit) {
            try {
                cmd = consoleReader.readLine().trim();
            } catch (IOException e1) {
                e1.printStackTrace();
                break;
            }

            doWork(cmd);
        }

        try {
            multicastReceiverThread.interrupt();
            multicastReceiverThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }





        //HTTP
        /*try {

            get("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/user/" + username + "/wallet/0");
            get("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/user/" + username + "/wallet/1"); // in bitcoin

            get("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/post/0");

            var postComment = new HashMap<String, String>() {{
                put("comment", "COMMENTONE");
            }};
    
            var commentObjectMapper = new ObjectMapper();
            String commentPostRequestBody = commentObjectMapper.writeValueAsString(postComment);

            put("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/post/0/action/comment",
                commentPostRequestBody);

            put("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/post/0/action/unvote", null);
            put("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/post/0/action/vote", null);

            get("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/post/0");

            try{
                Thread.sleep(15000);
            }catch(InterruptedException ignored){}

            get("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/user/" + username + "/wallet/0");
            get("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/user/" + username + "/wallet/1"); // in bitcoin

            put("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/post/0/action/comment",
                commentPostRequestBody);

            try{
                Thread.sleep(15000);
            }catch(InterruptedException ignored){}
            get("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/user/" + username + "/wallet/0");
            get("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/user/" + username + "/wallet/1"); // in bitcoin

            //delete("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/post/0");

            //get("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/users/cinema");


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/
    }


    private static void doWork(String cmd) {
        // \\s+ almeno uno spazio, \\s* 0 o più spazi, .* 0 o più caretteri qualsiasi
        // quit client
        if (Pattern.matches("^quit\\s*$", cmd)) {
            quit = true;
            if(username!=null)
                sendLogoutRequest();
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
                serverRMIObj.register(param[1], param[2], tags);
            } catch (RemoteException | DatabaseException e) {
                // TODO Auto-generated catch block
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
                e.printStackTrace();
                setUsernameAndPassword(null, null);
            }catch(AlreadyConnectedException e1){
                e1.printStackTrace();
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
                NetworkInterface netInt = NetworkInterface.getByName("wlan1");
                MulticastSocket multicast_skt = new MulticastSocket();
                multicast_skt.joinGroup(group, netInt);

                // start notification receiver
                multicastReceiverThread = new Thread(new MulticastNotificationReceiver(multicast_skt));
                multicastReceiverThread.start();

                System.out.println("User "+ username + " logged in");
            }catch(JsonProcessingException e){
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
            //TODO
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
            //TODO
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
            //TODO
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
            //TODO
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
            //TODO
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
            //TODO
            return;
        }

        /**
         * Comando: post \n <title> \n <content>
         * operazione per pubblicare un nuovo post. L’utente deve fornire titolo e
         * contenuto del post. Il titolo ha lunghezza massima di 20 caratteri e il contenuto una lunghezza massima di
         * 500 caratteri. Se l’operazione va a buon fine, il post è creato e disponibile per i follower dell’autore del post.
         * Il sistema assegna un identificatore univoco a ciascun post creato (idPost).
         */
        if (Pattern.matches("^post\\s*$", cmd)) {
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }
            System.out.printf("Title: ");
            HashMap<String, String> post;
            try {
                final String title = consoleReader.readLine().trim();
                if(title.length()>20){
                    System.out.println("Title max length: 20 characters. Operation rejected.");
                    return;
                }
                System.out.printf("Content: ");
                final String content = consoleReader.readLine().trim();
                if(content.length()>500){
                    System.out.println("Content max length: 500 characters. Operation rejected.");
                    return;
                }
                post = new HashMap<String, String>() {{
                    put("title", title);
                    put("content", content);
                }};
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }

            var postObjectMapper = new ObjectMapper();
            String createPostRequestBody;
            try {
                createPostRequestBody = postObjectMapper.writeValueAsString(post);
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
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
                    e.printStackTrace();
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
            //TODO
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
            //TODO
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
            //TODO
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
            //TODO
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
            //TODO
            return;
        }

        /**
         * Comando: comment <idPost> <comment>
         * operazione per aggiungere un commento ad un post. Se l’utente ha il post
         * nel proprio feed, il commento viene accettato, negli altri casi (ad es. l’utente non ha il post nel proprio feed
         * oppure è l’autore del post) il commento non viene accettato e il server restituisce un messaggio di errore.
         * Un utente può aggiungere più di un commento ad un post
         */
        if (Pattern.matches("^comment\\s+\\S+\\s*$", cmd)){
            if (username == null) {
                System.out.println("User is not logged in."); 
                return;
            }
            //TODO
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
            //TODO
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
            //TODO
            return;
        }

        System.out.println("Comando non valido.");
        //TODO: stampare usage
    }

    private static void setUsernameAndPassword(String username1, String password1) {
        username = username1;
        password = password1;
    }

    private static String sendLogoutRequest() {
        try {
            return HttpRequests.put("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/logout", null,
                                    username, password);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.out.println(e.getMessage());
            return null;
        }
    }
    private static String sendLoginRequest()  {
        try {
            return HttpRequests.put("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/login", null,
                                    username, password);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.out.println(e.getMessage());
            return null;
        }
    }
    private static String createPostRequest(String body)  {
        try {
            return HttpRequests.post("http://"+ client_config.SERVER_IP +":"+ client_config.HTTP_SERVER_PORT + "/post", 
                                    body, username, password);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.out.println(e.getMessage());
            return null;
        }
    }
    
    
    
    
    /**
     * Get server configuration values.
     */
    private static ClientConfig getClientConfig() throws StreamReadException, DatabindException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        if (Constants.CONFIG_FILE_PATH.exists()) {
            BufferedReader stateReader = new BufferedReader(new FileReader(Constants.CONFIG_FILE_PATH));
            return mapper.readValue(stateReader, new TypeReference<ClientConfig>(){});
        }
        return new ClientConfig();
    }


    @NoArgsConstructor
    private @Data static class ServerResponse{
        private String message;
    }

    @NoArgsConstructor
    @EqualsAndHashCode(callSuper=false)
    private @Data static class MulticastGroupData extends ServerResponse{
        private String multicast_group_ip;
        private String multicast_group_port;
    }

    @NoArgsConstructor
    private @Data static class PostIdData{
        private String postID;
    }

}
