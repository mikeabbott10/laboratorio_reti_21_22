package client.util;

import java.io.File;
import java.text.SimpleDateFormat;

public class Constants {
    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");;

    public static File CONFIG_FILE_PATH = new File("./client/clientConfig.json");

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
