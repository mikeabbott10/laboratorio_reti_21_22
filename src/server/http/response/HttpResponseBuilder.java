package server.http.response;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Map;
import java.util.Set;

import server.util.Logger;

/**
 * A simple class that is able to write {@link HttpResponse} over the socket.
 */
public final class HttpResponseBuilder {
    private static final Logger LOGGER = new Logger(HttpResponseBuilder.class.getName());
    private final Charset charset = Charset.forName("UTF-8");
    private final CharsetEncoder encoder = charset.newEncoder();

    public byte[] buildHeaders(WritableByteChannel channel, HttpResponse response) throws IOException {
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

    public ByteBuffer buildContent(WritableByteChannel channel, HttpResponse response){
        if (!response.hasPendingContent()) {
            return null;
        }
        
        byte[] content = response.flushPendingContent();
        ByteBuffer byteBuffer = ByteBuffer.wrap(content);
        return byteBuffer;
    }
}