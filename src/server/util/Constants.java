package server.util;

import java.io.File;
import java.text.SimpleDateFormat;

/**
 * A simple container for various constants.
 */
public interface Constants {
    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("LL-dd-yyyy hh:mm:ss:SS");;
    //http
    String[] SUPPORTED_HTTP_METHODS = {"GET", "POST", "PUT", "DELETE"};
    String SUPPORTED_HTTP_VERSION = "HTTP/1.1";

    int SOCKET_READ_DATA_LIMIT_BYTES = 32768;
    int SOCKET_READ_BUFFER_SIZE_BYTES = 8192;

    public File CONFIG_FILE_PATH = new File("./server/serverConfig.json");

    int CLEANUP_TIMEOUT = 20000;


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