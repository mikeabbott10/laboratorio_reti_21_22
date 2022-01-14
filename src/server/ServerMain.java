package server;

import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import database.Database;
import database.DatabaseImpl;
import database.social.SocialService;
import server.backup.BackupDaemon;
import server.multicast.RewardDaemon;
import server.nio.NIOServer;
import server.rmi.ServerRMIImplementation;
import server.rmi.ServerRMIInterface;
import server.util.Constants;
import server.util.Logger;
import server.util.ServerConfig;

public class ServerMain {
    private static Logger LOGGER = new Logger(ServerMain.class.getName());

    public static InetAddress localhostAddr;
    
    public static Database db;
    private static SocialService social;

    public static ServerRMIImplementation serverRMIService;

    private static NIOServer nio;
    public static boolean quit;

    public static ServerConfig server_config; // get it from file
    
    public static void main(String[] args) throws Exception {
        quit = false;
        
        server_config = getServerConfig();

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
        nio = new NIOServer(server_config.HTTP_SERVER_PORT, db);
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
     * Get server configuration values.
     */
    private static ServerConfig getServerConfig() throws StreamReadException, DatabindException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        if (Constants.CONFIG_FILE_PATH.exists()) {
            BufferedReader stateReader = new BufferedReader(new FileReader(Constants.CONFIG_FILE_PATH));
            return mapper.readValue(stateReader, new TypeReference<ServerConfig>(){});
        }
        return new ServerConfig();
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
            quit = true;
            if(nio.getSelector() != null) 
                nio.getSelector().wakeup();
            System.out.println("Termination signal handled");
        };
    }

    private static void startRMIService() throws RemoteException, MalformedURLException{
        serverRMIService = new ServerRMIImplementation(db);
        // Esportazione dell'Oggetto 
        ServerRMIInterface stub = 
            (ServerRMIInterface) UnicastRemoteObject.exportObject(serverRMIService, 0);
        // Creazione di un registry sulla porta PORT
        LocateRegistry.createRegistry(server_config.SERVER_RMI_PORT);
        // Pubblicazione dello stub nel registry
        Naming.rebind(server_config.RMIServerUrl + server_config.rmiServiceName, stub);
        //System.out.println("Server ready");
    }

}
