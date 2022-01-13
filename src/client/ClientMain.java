package client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import server.rmi.ServerRMIInterface;

public class ClientMain {
    public static final int BUF_SIZE = 4096;

    private static final int SERVER_RMI_PORT = 25258;
    private static final int HTTP_SERVER_PORT = 8080;
    private static final String SERVER_IP = "localhost";
    private static final String serverUrl = "rmi://"+ SERVER_IP +":" + SERVER_RMI_PORT;
    private static final String rmiServiceName = "/winsomeservice";

    private static final String nomeUtenteProvvisorio = "nomeUtente1";
    private static final String passUtenteProvvisoria = "passUtente1";

    public static void main(String args[]){
        // RMI
        try {

            // System.out.println("Looking for server");
            ServerRMIInterface serverRMIObj = (ServerRMIInterface) Naming.lookup(serverUrl + rmiServiceName);

            // System.out.println("Registering for callback");
            ClientNotifyEventInterface callbackObj = new ClientNotifyEventImplementation();
            ClientNotifyEventInterface stub = 
                (ClientNotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);

            serverRMIObj.register(nomeUtenteProvvisorio, passUtenteProvvisoria, new String[]{"art","cinema"});
            serverRMIObj.registerForCallback(stub, nomeUtenteProvvisorio, passUtenteProvvisoria);

            // attende gli eventi generati dal server per
            // un certo intervallo di tempo;
            //Thread.sleep (10000);

            // System.out.println("Unregistering for callback");
            serverRMIObj.unregisterForCallback(stub);
        } catch (Exception e){ 
            e.printStackTrace();
            System.err.println("Client exception:"+ e.getMessage());
        }

        try {
            var post = new HashMap<String, String>() {{
                put("title", "TITOLONE");
                put("content", "CONTENUTONE");
            }};
    
            var postObjectMapper = new ObjectMapper();
            String createPostRequestBody = postObjectMapper.writeValueAsString(post);

            post("http://"+ SERVER_IP +":"+ HTTP_SERVER_PORT + "/post", 
                createPostRequestBody);

            get("http://"+ SERVER_IP +":"+ HTTP_SERVER_PORT + "/user/" + nomeUtenteProvvisorio + "/wallet/0");
            get("http://"+ SERVER_IP +":"+ HTTP_SERVER_PORT + "/user/" + nomeUtenteProvvisorio + "/wallet/1"); // in bitcoin

            get("http://"+ SERVER_IP +":"+ HTTP_SERVER_PORT + "/post/0");

            var postComment = new HashMap<String, String>() {{
                put("comment", "COMMENTONE");
            }};
    
            var commentObjectMapper = new ObjectMapper();
            String commentPostRequestBody = commentObjectMapper.writeValueAsString(postComment);

            put("http://"+ SERVER_IP +":"+ HTTP_SERVER_PORT + "/post/0/action/comment",
                commentPostRequestBody);

            put("http://"+ SERVER_IP +":"+ HTTP_SERVER_PORT + "/post/0/action/unvote", null);
            put("http://"+ SERVER_IP +":"+ HTTP_SERVER_PORT + "/post/0/action/vote", null);

            get("http://"+ SERVER_IP +":"+ HTTP_SERVER_PORT + "/post/0");

            try{
                Thread.sleep(15000);
            }catch(InterruptedException ignored){}

            get("http://"+ SERVER_IP +":"+ HTTP_SERVER_PORT + "/user/" + nomeUtenteProvvisorio + "/wallet/0");
            get("http://"+ SERVER_IP +":"+ HTTP_SERVER_PORT + "/user/" + nomeUtenteProvvisorio + "/wallet/1"); // in bitcoin

            put("http://"+ SERVER_IP +":"+ HTTP_SERVER_PORT + "/post/0/action/comment",
                commentPostRequestBody);

            try{
                Thread.sleep(15000);
            }catch(InterruptedException ignored){}
            get("http://"+ SERVER_IP +":"+ HTTP_SERVER_PORT + "/user/" + nomeUtenteProvvisorio + "/wallet/0");
            get("http://"+ SERVER_IP +":"+ HTTP_SERVER_PORT + "/user/" + nomeUtenteProvvisorio + "/wallet/1"); // in bitcoin

            //delete("http://"+ SERVER_IP +":"+ HTTP_SERVER_PORT + "/post/0");

            //get("http://"+ SERVER_IP +":"+ HTTP_SERVER_PORT + "/users/cinema");


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public static void get(String uri) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .headers("username", nomeUtenteProvvisorio, "password", passUtenteProvvisoria)
                .uri(URI.create(uri))
                .GET()
                .build();
    
        HttpResponse<String> response =
              client.send(request, BodyHandlers.ofString());
    
        System.out.println(response.body());
    }

    public static void delete(String uri) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .headers("username", nomeUtenteProvvisorio, "password", passUtenteProvvisoria)
                .uri(URI.create(uri))
                .DELETE()
                .build();
    
        HttpResponse<String> response =
              client.send(request, BodyHandlers.ofString());
    
        System.out.println(response.body());
    }

    public static void put(String url, String parameters) throws Exception {
        byte[] putData = null;
        if(parameters!=null) 
            putData = parameters.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection con = null;
        try {
            var myurl = new URL(url);
            con = (HttpURLConnection) myurl.openConnection();

            con.setDoOutput(true);
            con.setRequestMethod("PUT");
            con.setRequestProperty("User-Agent", "Java client");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("username", nomeUtenteProvvisorio);
            con.setRequestProperty("password", passUtenteProvvisoria);

            if(parameters!=null){
                try (var wr = new DataOutputStream(con.getOutputStream())) {
                    wr.write(putData);
                }
            }

            StringBuilder content;

            try (var br = new BufferedReader(
                    new InputStreamReader(con.getInputStream()))) {
                String line;
                content = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            }
            System.out.println(content.toString());

        } finally {
            con.disconnect();
        }
    }

    public static void post(String url, String parameters) throws Exception {
        /*HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .headers("username", nomeUtenteProvvisorio, "password", passUtenteProvvisoria,
                        "Content-Type", "application/x-www-form-urlencoded")
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(parameters))
                .build();
    
        HttpResponse<String> response =
              client.send(request, BodyHandlers.ofString());
    
        System.out.println(response.body());*/

        // POST request with java 11 HttpClient seems bugged. Use HttpURLConnection instead

        byte[] postData = parameters.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection con = null;
        try {

            var myurl = new URL(url);
            con = (HttpURLConnection) myurl.openConnection();

            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Java client");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("username", nomeUtenteProvvisorio);
            con.setRequestProperty("password", passUtenteProvvisoria);

            try (var wr = new DataOutputStream(con.getOutputStream())) {

                wr.write(postData);
            }

            StringBuilder content;

            try (var br = new BufferedReader(
                    new InputStreamReader(con.getInputStream()))) {

                String line;
                content = new StringBuilder();

                while ((line = br.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            }

            System.out.println(content.toString());

        } finally {
            con.disconnect();
        }

    }
}
