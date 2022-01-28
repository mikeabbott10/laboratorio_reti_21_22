package server.nio;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class CustomRequest {
    private Selector selector;
    private SocketChannel client_channel;
    private String message;
    private SelectionKey key;

    public CustomRequest(Selector selector, SocketChannel client_channel, 
            String message, SelectionKey key) {
        this.selector = selector;
        this.client_channel = client_channel;
        this.message = message;
        this.key = key;
    }

    public Selector getSelector() {
        return selector;
    }
    public void setSelector(Selector selector) {
        this.selector = selector;
    }

    public SocketChannel getClient_channel() {
        return client_channel;
    }
    public void setClient_channel(SocketChannel client_channel) {
        this.client_channel = client_channel;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public SelectionKey getKey() {
        return key;
    }
    public void setKey(SelectionKey key) {
        this.key = key;
    }
}
