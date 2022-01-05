package server;

import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

import database.Database;
import database.DatabaseImpl;
import server.nio.NIOServer;
import server.rmi.ServerRMIImplementation;
import server.rmi.ServerRMIInterface;
import social.SocialService;

public class ServerMain {
    // rmi
    private static final int SERVER_RMI_PORT = 25258;
    private static final int NIO_SERVER_PORT = 25268;
    private static final String SERVER_IP = "localhost";
    private static final String serverUrl = "rmi://"+ SERVER_IP +":" + SERVER_RMI_PORT;
    private static final String rmiServiceName = "/winsomeservice";

    //tcp
    public final static int TCP_SERVICE_PORT = 6789;
    public static InetAddress localhostAddr;
    
    public static Database db;
    private static SocialService social;
    
    public static void main(String[] args) throws Exception {
        // TODO: server state from json files
        social = new SocialService();
        db = new DatabaseImpl(social);
        try{
            startRMIService();
        }catch (MalformedURLException | RemoteException e) {
            System.out.println("Communication error " + e.toString());
        }

        // startTCPService
        new NIOServer(NIO_SERVER_PORT, db).start();
        
    }

    private static void startRMIService() throws RemoteException, MalformedURLException{
        ServerRMIImplementation serverService = new ServerRMIImplementation(db);
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
