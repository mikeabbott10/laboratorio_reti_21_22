package server;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class CustomRequest {
    protected Selector selector;
    protected SocketChannel client_channel;
    protected String message;
    protected SelectionKey key;

    public CustomRequest(Selector selector, SocketChannel client_channel, 
            String message, SelectionKey key) {
        this.selector = selector;
        this.client_channel = client_channel;
        this.message = message;
        this.key = key;
    }
    
}
