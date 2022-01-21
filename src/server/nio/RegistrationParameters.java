package server.nio;

import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class RegistrationParameters {
    protected Selector selector;
    protected SocketChannel clientChannel;
    protected int operation;
    protected ByteBuffer bb;
    
    public RegistrationParameters(Selector sel, SocketChannel client_channel, int op, ByteBuffer bb) {
        this.selector = sel;
        this.clientChannel = client_channel;
        this.operation = op;
        this.bb = bb;
    }
}
