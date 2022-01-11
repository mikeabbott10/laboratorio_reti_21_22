package client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;

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
            ServerRMIInterface server = (ServerRMIInterface) Naming.lookup(serverUrl + rmiServiceName);

            // System.out.println("Registering for callback");
            ClientNotifyEventInterface callbackObj = new ClientNotifyEventImplementation();
            ClientNotifyEventInterface stub = 
                (ClientNotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);

            server.register(nomeUtenteProvvisorio, passUtenteProvvisoria, new String[]{"art","cinema"});
            server.registerForCallback(stub, nomeUtenteProvvisorio, passUtenteProvvisoria);
            
            // attende gli eventi generati dal server per
            // un certo intervallo di tempo;
            //Thread.sleep (10000);

            // System.out.println("Unregistering for callback");
            server.unregisterForCallback(stub);
        } catch (Exception e){ 
            e.printStackTrace();
            System.err.println("Client exception:"+ e.getMessage());
        }

        try {
            get("http://"+ SERVER_IP +":"+ HTTP_SERVER_PORT + "/user/NomeUtente");
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
}
