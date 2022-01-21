package server.backup;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import server.util.Logger;
import database.Database;
import database.DatabaseImpl;
import database.social.SocialService;
import server.ServerMain;

public class BackupDaemon implements Runnable{
    private Logger LOGGER = new Logger(BackupDaemon.class.getName());

    private Database db;
    public static File backupFile = new File(ServerMain.server_config.BACKUP_DIRECTORY+ "socialNetworkState.json");

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
                Thread.sleep(ServerMain.server_config.BACKUP_TIMEOUT);
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
        backupRoutine(); // once more after all stopped
        return;
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
            SimpleBeanPropertyFilter theFilter = SimpleBeanPropertyFilter
                .serializeAllExcept("loggedUsers");
            SimpleFilterProvider filters = new SimpleFilterProvider()
                .addFilter("socialFilter", theFilter);
            filters.setFailOnUnknownId(false);
            mapper.writer(filters).writeValue(backupFile, new SocialService(db.getSocialInstance(), db));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    
}
