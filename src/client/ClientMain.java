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

    public static void main(String[] args){
        quit = false;
        try {
            Client client = new Client(getClientConfig());
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
}
