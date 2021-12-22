package server;
// RMI
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

import social.SocialService;

public class ServerMain {
    // rmi
    private static final int SERVER_RMI_PORT = 25258;
    private static final int SERVER_TCP_PORT = 25268;
    private static final String SERVER_IP = "localhost";
    private static final String serverUrl = "rmi://"+ SERVER_IP +":" + SERVER_RMI_PORT;
    private static final String rmiServiceName = "/winsomeservice";

    //tcp
    public final static int TCP_SERVICE_PORT = 6789;
    public static InetAddress localhostAddr;
    
    public static SocialService social;
    
    public static void main(String[] args) throws Exception {
        social = new SocialService();
        try{
            startRMIService();
        }catch (MalformedURLException | RemoteException e) {
            System.out.println("Communication error " + e.toString());
        }

        // startTCPService
        new NIOServer(SERVER_TCP_PORT).start();
        
    }

    private static void startRMIService() throws RemoteException, MalformedURLException{
        ServerRMIImplementation serverService = new ServerRMIImplementation(social);
        // Esportazione dell'Oggetto 
        ServerRMIInterface stub = 
            (ServerRMIInterface) UnicastRemoteObject.exportObject(serverService, 0);
        // Creazione di un registry sulla porta PORT
        LocateRegistry.createRegistry(SERVER_RMI_PORT);
        // Pubblicazione dello stub nel registry
        Naming.rebind(serverUrl + rmiServiceName, stub);
        //System.out.println("Server ready");
    }

}
