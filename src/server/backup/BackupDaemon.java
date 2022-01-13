package server.backup;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import server.util.Logger;

import database.Database;
import server.ServerMain;
import server.util.Constants;

public class BackupDaemon implements Runnable{
    private Logger LOGGER = new Logger(BackupDaemon.class.getName());

    private Database db;
    public static File backupFile = new File(Constants.BACKUP_DIRECTORY+ "socialNetworkState.json");

    private Thread rewardThread;
    

    public BackupDaemon(Thread rewardThread, Database db) {
        this.rewardThread = rewardThread;
        this.db = db;

        /*Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        System.out.println("Current absolute path is: " + s);*/
    }

    @Override
    public void run() {
        while (!ServerMain.quit && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(Constants.BACKUP_TIMEOUT);
            } catch (InterruptedException ignored) {}
            if (!Thread.currentThread().isInterrupted()) {
                backupRoutine();
            }
        }
        // we want to be sure to perform backup on exit
        try {
            rewardThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        backupRoutine();
        return;
    }

    /**
     * Creates files if they do not exist
    */
    private void createFiles(File[] files){
        Arrays.asList(files).forEach((bkp) -> {
            if (!bkp.exists()){
                try {
                    bkp.createNewFile();
                    //LOGGER.info(bkp.getAbsolutePath());
                } catch (IOException e) {
                    LOGGER.warn("Error creating backup files");
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Backup routine
     */
    private void backupRoutine() {
        createFiles(new File[]{ backupFile });
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        mapper.enable(SerializationFeature.INDENT_OUTPUT); // indent the output decent way
        try {
            SimpleFilterProvider no_filter_sfp = new SimpleFilterProvider();
            no_filter_sfp.setFailOnUnknownId(false);
            mapper.writer(no_filter_sfp).writeValue(backupFile, db.getSocialInstance());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
