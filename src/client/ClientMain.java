package client;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import client.util.ClientConfig;
import client.util.Constants;

public class ClientMain {
    public static boolean quit;
    static Client client;
    
    public static void main(String[] args){
        quit = false;
        // termination handling
        Runtime.getRuntime().addShutdownHook(new Thread(signalHandler(Thread.currentThread())));

        boolean test = false;
        if(args.length > 0){
            // test environment
            test = true;
            args[0] = args[0].replace("\"", "");
            args[args.length-1] = args[args.length-1].replace("\"", "");
        }
        try {
            client = test ? new Client(getClientConfig(), String.join(" ", args)) : new Client(getClientConfig());
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Bye!");
        System.exit(0);
    }

    /**
     * Get server configuration values.
     */
    private static ClientConfig getClientConfig() throws StreamReadException, DatabindException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        if (Constants.CONFIG_FILE_PATH.exists()) {
            BufferedReader stateReader = new BufferedReader(new FileReader(Constants.CONFIG_FILE_PATH));
            return mapper.readValue(stateReader, new TypeReference<ClientConfig>(){});
        }
        return new ClientConfig();
    }

    private static Runnable signalHandler(Thread currentThread) {
        return () -> {
            // Unregistering for callback
            try {
                client.serverRMIObj.unregisterForCallback(client.stub);
                if(client.username!=null)
                    client.sendLogoutRequest();
            } catch (Exception e) {
                //e.printStackTrace();
            }
            //System.out.println("Termination signal handled");
        };
    }
}
