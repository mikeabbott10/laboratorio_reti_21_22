package server;

import java.nio.*; 
import java.nio.channels.*;
import java.net.*; 
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;

public class NIOServer {
    public final int BUF_SIZE = 5;
    private final byte[] echoedMsgBytes = "Echoed by server: ".getBytes();
    private final ByteBuffer echoedMsgBB;
    private final int port;
    private ConcurrentHashMap<SocketChannel, ChannelData> channelToDataMap;

    
    public NIOServer(int port) {
        this.port = port;
        this.channelToDataMap = new ConcurrentHashMap<>();
        // directly allocated buffer : this buffer will live in kernel space (no user space buffer means 
        // less time to access the buffer. We have no copies in user space)
        echoedMsgBB = ByteBuffer.allocateDirect( echoedMsgBytes.length ).put( echoedMsgBytes ).flip();
        echoedMsgBB.mark(); // salva la posizione 0 a cui ritornare (tramite metodo reset()) una volta 
    }

    public void start(){
        try( 
            ServerSocketChannel serverChannel = ServerSocketChannel.open() 
        ){
            serverChannel.socket().bind( new InetSocketAddress(port) );
            serverChannel.configureBlocking(false); // channel in non blocking mode
            Selector selector = Selector.open();
            // add the server channel to the selector (OP_ACCEPT operation is registered)
            serverChannel.register(selector, SelectionKey.OP_ACCEPT); 

            while (true) {
                selector.select();
                Set<SelectionKey> readyKeys = selector.selectedKeys(); // set of ready channel keys 
                Iterator <SelectionKey> iterator = readyKeys.iterator();
    
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    // removes the key from Selected Set, not from registered Set
                    try {
                        if (key.isAcceptable()) { // key channel is ready for being accepted
                            ServerSocketChannel server = (ServerSocketChannel) key.channel();
                            SocketChannel client = server.accept(); // non blocking!
                            //System.out.println("Accepted connection from " + client);
                            client.configureBlocking(false);
    
                            // register the client SocketChannel to the selector with a
                            // focus on the read operation (we want to read something now) 
                            registerRead(selector, client);
    
                        }else if (key.isWritable()) { // key channel is ready for being written
                            SocketChannel client = (SocketChannel) key.channel();
                            String output = (String) key.attachment();
                            ByteBuffer answer = ByteBuffer.wrap(output.getBytes());
                            client.write(answer);
                            if(!answer.hasRemaining())
                                registerRead(selector, client); // i want to read something now
    
                        }else if (key.isReadable()) { // key channel is ready for being read
                            readClientMessage(selector, key);
                        }
                    }catch (IOException ex) { // client suddenly closed
                        ex.printStackTrace();
                        key.channel().close();
                        key.cancel();
                    }
                }
            }
        }catch (IOException ex) {
            ex.printStackTrace();
        }        
    }

    /**
     * Register the interest to the read operation on the selector sel
     *
     * @param sel the selector used by the server
     * @param client_channel client socket channel
     * @throws IOException
     */
    private void registerRead(Selector sel, SocketChannel client_channel) throws IOException{
        // create the buffer
        ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
        ByteBuffer message = ByteBuffer.allocate(BUF_SIZE);
        ByteBuffer[] bfs = {length, message};
        // add the client channel to the selector (OP_READ operation is registered)
        // and adds bytebuffer array [length, message] as attachment
        client_channel.register(sel, SelectionKey.OP_READ, bfs);
    }

    /**
     * Read the message sent by the client and register the interest to 
     * the write operation on the selector sel
     *
     * @param sel the selector used by the server
     * @param key selection key
     * @throws IOException
     */
    private void readClientMessage(Selector sel, SelectionKey key) throws IOException {
        /*
         * accetta una nuova connessione creando un SocketChannel per la comunicazione con
         * il client che la richiede
         */
        SocketChannel client_channel = (SocketChannel) key.channel();
        
        ByteBuffer[] bfs = null;
        ByteBuffer bfs2 = null;
        long payload_length;
        long readMsgBytes;
        ChannelData thisClientPreviuosData = null;
        String message = "";

        try{ // Invariant: here if channelToDataMap.get(client_channel) == null
            bfs = (ByteBuffer[]) key.attachment(); // recupera l'array di bytebuffer (attachment)
            // check quit condition
            if( client_channel.read(bfs) == -1){
                System.out.println("Server: chiusa la connessione con il client " 
                                                        + client_channel.getRemoteAddress());
                key.cancel();
                client_channel.close();
            }

            if(bfs[0].hasRemaining()){
                // the whole message size didn't arrive, reject the client.
                // No other handler for this 
                key.cancel();
                client_channel.close();
                return;
            }
            bfs[0].flip(); // from write mode to read mode
            payload_length = bfs[0].getInt(); // get the payload length
            readMsgBytes = bfs[1].position(); // number of read message bytes

            bfs[1].flip();
            message = new String(bfs[1].array()).trim();
        }
        catch(ClassCastException ignored){ 
            bfs2 = (ByteBuffer) key.attachment(); // recupera bytebuffer (attachment)
            // check quit condition
            if( client_channel.read(bfs2) == -1){
                System.out.println("Server: chiusa la connessione con il client " 
                                        + client_channel.getRemoteAddress());
                key.cancel();
                client_channel.close();
            }

            thisClientPreviuosData = channelToDataMap.get(client_channel);
            // Invariant: here if channelToDataMap.get(client_channel) != null
            assert thisClientPreviuosData != null;

            payload_length = thisClientPreviuosData.getPayload_length();
            readMsgBytes = bfs2.position(); // number of read message bytes

            bfs2.flip();
            message = 
                thisClientPreviuosData.getMessage() + new String(bfs2.array()).trim();

        }
        
        boolean noMoreReadsNeeded = 
            ( thisClientPreviuosData == null && readMsgBytes == payload_length ) ||
            ( thisClientPreviuosData != null && 
                readMsgBytes + thisClientPreviuosData.getPreviously_read_bytes() >= // == 
                                            thisClientPreviuosData.getPayload_length() );

        //debug
        // System.out.printf("readMsgBytes:\t%d\n", readMsgBytes);
        // System.out.printf("payload_length:\t%d\n", payload_length);
        // if(thisClientPreviuosData != null){
        //     System.out.printf("thisClientPreviuosData.getPreviously_read_bytes():\t%d\n", 
        //         thisClientPreviuosData.getPreviously_read_bytes());
        //     System.out.printf("thisClientPreviuosData.getPayload_length():\t%d\n",
        //         thisClientPreviuosData.getPayload_length());
        // }
        

        if ( noMoreReadsNeeded ) { // we received the whole message
            channelToDataMap.remove(client_channel);
            
            System.out.printf("Server: ricevuto tutto:\n%s\n", message);

            // TODO: we want to do things here. We just got a message

            /*
            * aggiunge il canale del client al selector con l'operazione OP_WRITE
            * e aggiunge il messaggio ricevuto come attachment (aggiungendo la risposta addizionale)
            */
            client_channel.register(sel, SelectionKey.OP_WRITE, "OKTUTTOAPPOSTO");
        }else{ // we didn't read the whole message yet
            // save the payload length we need somewhere we can reach it after 
            // (Use hash map <SocketChannel, ChannelData>)

            ChannelData cd = new ChannelData();
            if( thisClientPreviuosData != null ){
                cd.setPayload_length( thisClientPreviuosData.getPayload_length() );
                cd.setPreviously_read_bytes(
                    readMsgBytes + thisClientPreviuosData.getPreviously_read_bytes());
                cd.setMessage(message);
            }else{
                cd.setPayload_length(payload_length);
                cd.setPreviously_read_bytes( readMsgBytes );
                cd.setMessage( message );
            }
            channelToDataMap.put(client_channel, cd);
            System.out.printf("Server: ricevuto \n%s\n", message);
            client_channel.register(sel, SelectionKey.OP_READ, ByteBuffer.allocate(BUF_SIZE));
        }
        
    }

    /**
     * Write the attached buffer to the channel related to key 
     *
     * @param sel select
     * @param key chiave di selezione
     * @throws IOException
     */
    private void answerToClient(Selector sel, SelectionKey key) throws IOException {
        SocketChannel c_channel = (SocketChannel) key.channel();
        String echoAnsw = (String) key.attachment();
        ByteBuffer bbEchoAnsw = ByteBuffer.wrap(echoAnsw.getBytes());
        // TODO: metti in un ciclo la write? oppure lascia fare al selector per la scrittura successiva?
        c_channel.write(bbEchoAnsw);
        System.out.println("Server: " + echoAnsw + " inviato al client " + c_channel.getRemoteAddress());
        if (!bbEchoAnsw.hasRemaining()) {
            bbEchoAnsw.clear();
            this.registerRead(sel, c_channel);
        }
    }

}