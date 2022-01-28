package server.backup;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import database.Database;
import database.social.SocialService;
import server.ServerMain;

/**
 * Save the server state on file with this routine.
 */
public class BackupRoutine implements Runnable{
    private Database db;
    public static File backupFile = new File(ServerMain.server_config.BACKUP_DIRECTORY+ "socialNetworkState.json");
    private Thread rewardThread;

    public BackupRoutine(Thread rewardThread, Database db) {
        this.rewardThread = rewardThread;
        this.db = db;
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
            //e.printStackTrace();
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
                .serializeAllExcept("loggedUsers", "newUpvotes", "newDownvotes", "newComments");
            SimpleFilterProvider filters = new SimpleFilterProvider()
                .addFilter("socialFilter", theFilter);
            filters.setFailOnUnknownId(false);
            mapper.writer(filters).writeValue(backupFile, new SocialService(db.getSocialInstance(), db));
        } catch (IOException e) {
            //e.printStackTrace();
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
                    //System.out.println(bkp.getAbsolutePath());
                } catch (IOException e) {
                    System.out.println("Error creating backup files");
                    //e.printStackTrace();
                }
            }
        });
    }
    
}
