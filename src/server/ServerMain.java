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
    private static final int HTTP_SERVER_PORT = 8080;
    private static final String SERVER_IP = "localhost";
    private static final String serverUrl = "rmi://"+ SERVER_IP +":" + SERVER_RMI_PORT;
    private static final String rmiServiceName = "/winsomeservice";

    //tcp
    public final static int TCP_SERVICE_PORT = 6789;
    public static InetAddress localhostAddr;
    
    public static Database db;
    private static SocialService social;

    private static NIOServer nio;
    public static boolean quit;
    
    public static void main(String[] args) throws Exception {
        quit = false;
        // TODO: server state from json files
        social = new SocialService();
        db = new DatabaseImpl(social);

        // termination handling
        Runtime.getRuntime().addShutdownHook(new Thread(signalHandler(Thread.currentThread())));

        //rmi
        try{
            startRMIService();
        }catch (MalformedURLException | RemoteException e) {
            System.out.println("Communication error " + e.toString());
        }

        // nio, http server
        nio = new NIOServer(HTTP_SERVER_PORT, db);
        nio.start();
        
    }

    private static Runnable signalHandler(Thread currentThread) {
        return () -> {
            //System.out.println("Termination signal occurred");
            quit = true;
            if(nio.getSelector() != null) 
                nio.getSelector().wakeup();
            try{
                currentThread.join();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        };
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
