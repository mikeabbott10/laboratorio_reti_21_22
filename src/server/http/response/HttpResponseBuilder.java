package server.http.response;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

import server.util.Logger;

/**
 * A simple class that is able to write {@link HttpResponse} over the socket.
 */
public final class HttpResponseBuilder {
    private static final Logger LOGGER = new Logger(HttpResponseBuilder.class.getName());

    public byte[] buildHeaders(HttpResponse response) {
        if (response.isWroteHeaders()) {
            return null;
        }

        String prefix = response.generatePrefix();
        StringBuilder sb = new StringBuilder(prefix);
        sb.append("\r\n");

        Set<Map.Entry<String, String>> headers = response.getHeaders().entrySet();
        for (Map.Entry<String, String> header : headers) {
            sb.append(header.getKey());
            sb.append(": ");
            sb.append(header.getValue());
            sb.append("\r\n");
        }
        sb.append("\r\n");

        response.markAsWroteHeaders();
        //LOGGER.info(sb.toString());
        return sb.toString().getBytes();
    }

    public ByteBuffer buildContent(HttpResponse response){
        if (!response.hasPendingContent()) {
            return null;
        }
        
        byte[] content = response.flushPendingContent();
        ByteBuffer byteBuffer = ByteBuffer.wrap(content);
        return byteBuffer;
    }
}