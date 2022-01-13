package server.util;

import java.text.SimpleDateFormat;

/**
 * A simple container for various constants.
 */
public interface Constants {
    // rmi
    static final int SERVER_RMI_PORT = 25258;
    static final String SERVER_IP = "localhost";
    static final String serverUrl = "rmi://"+ SERVER_IP +":" + SERVER_RMI_PORT;
    static final String rmiServiceName = "/winsomeservice";

    //tcp
    static final int HTTP_SERVER_PORT = 8080;
    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");;
    //http
    String[] SUPPORTED_HTTP_METHODS = {"GET", "POST", "PUT", "DELETE"};
    String SUPPORTED_HTTP_VERSION = "HTTP/1.1";

    //multicast
    int MULTICAST_PORT = 40000;
    String MULTICAST_ADDRESS = "239.255.1.3";

    double AUTHOR_PERCENTAGE = 0.7;
    String BACKUP_DIRECTORY = "./server/bkp/";
    long REWARD_TIMEOUT = 10000L;
    long BACKUP_TIMEOUT = 10000L;

    int SOCKET_READ_DATA_LIMIT_BYTES = 32768;
    int SOCKET_READ_BUFFER_SIZE_BYTES = 8192;


    enum HttpStatus {
        SUCCESS(200, "OK"),
        BAD_REQUEST(400, "Bad Request"),
        FORBIDDEN(403, "Forbidden"),
        REQUEST_TIMEOUT(408, "Request Timeout"),
        TOO_MANY_REQUESTS(429, "Too Many Requests"),
        NOT_FOUND(404, "Not Found");

        public final int code;
        public final String reason;

        HttpStatus(int code, String reason) {
            this.code = code;
            this.reason = reason;
        }

    }

}