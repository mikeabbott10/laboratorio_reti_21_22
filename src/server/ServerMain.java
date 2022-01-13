package server;

import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import database.Database;
import database.DatabaseImpl;
import server.backup.BackupDaemon;
import server.multicast.RewardDaemon;
import server.nio.NIOServer;
import server.rmi.ServerRMIImplementation;
import server.rmi.ServerRMIInterface;
import server.util.Constants;
import server.util.Logger;
import social.SocialService;

public class ServerMain {
    private static Logger LOGGER = new Logger(ServerMain.class.getName());

    public static InetAddress localhostAddr;
    
    public static Database db;
    private static SocialService social;

    public static ServerRMIImplementation serverRMIService;

    private static NIOServer nio;
    public static boolean quit;
    
    public static void main(String[] args) throws Exception {
        quit = false;
        
        // server state
        social = getServerStateFromBackup();
        db = new DatabaseImpl(social);

        // termination handling
        Runtime.getRuntime().addShutdownHook(new Thread(signalHandler(Thread.currentThread())));

        //rmi
        try{
            startRMIService();
        }catch (MalformedURLException | RemoteException e) {
            LOGGER.warn("Communication error " + e.toString());
        }

        //reward daemon (multicast)
        Thread rewardThread = new Thread(new RewardDaemon(Thread.currentThread(), db));
        rewardThread.start();

        //backup daemon
        Thread backupThread = new Thread(new BackupDaemon(rewardThread, db));
        backupThread.start();

        //nio, http server
        nio = new NIOServer(Constants.HTTP_SERVER_PORT, db);
        nio.start();

        try {
            rewardThread.interrupt();
            rewardThread.join();
            backupThread.interrupt();
            backupThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get previous state if exists
     */
    public static SocialService getServerStateFromBackup() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            if (BackupDaemon.backupFile.exists()) {
                BufferedReader stateReader = new BufferedReader(new FileReader(BackupDaemon.backupFile));
                return mapper.readValue(stateReader, new TypeReference<SocialService>(){});
            }
        } catch (IOException e) {
            LOGGER.warn("Failed restoring social network state : "+e.toString());
            e.printStackTrace();
        }
        return new SocialService();

        
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
        serverRMIService = new ServerRMIImplementation(db);
        // Esportazione dell'Oggetto 
        ServerRMIInterface stub = 
            (ServerRMIInterface) UnicastRemoteObject.exportObject(serverRMIService, 0);
        // Creazione di un registry sulla porta PORT
        LocateRegistry.createRegistry(Constants.SERVER_RMI_PORT);
        // Pubblicazione dello stub nel registry
        Naming.rebind(Constants.serverUrl + Constants.rmiServiceName, stub);
        //System.out.println("Server ready");
    }

}
