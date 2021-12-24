package server;

import java.nio.*; 
import java.nio.channels.*;
import java.net.*; 
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import social.SocialService;

import java.io.IOException;

public class NIOServer {
    private final int workersAmount = 4;
    private ArrayList<Thread> workers;
    
    private final int port;

    public final int BUF_SIZE = 5;
    protected ConcurrentHashMap<SocketChannel, ChannelData> channelToDataMap;
    protected LinkedBlockingQueue<CustomRequest> requestList;
    private SocialService social;

    public NIOServer(int port, SocialService social) {
        this.port = port;
        this.social = social;
        this.channelToDataMap = new ConcurrentHashMap<>();
        this.requestList = new LinkedBlockingQueue<>();
        this.social = social;
        this.workers = new ArrayList<>(workersAmount);
    }

    public void start(){
        startWorkers();

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
                            client.configureBlocking(false);
    
                            // register the client SocketChannel to the selector with a
                            // focus on the read operation (we want to read something now) 
                            registerOp(selector, client, SelectionKey.OP_READ, null);
    
                        }
                        else if (key.isWritable()) { // key channel is ready for being written
                            answerToClient(selector, key);
    
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
        
        joinWorkers();
    }
    
    /**
     * Read the message sent by the client and update the interest of the channel
     *
     * @param key selection key
     * @throws IOException
     */
    private void readClientMessage(Selector sel, SelectionKey key) throws IOException {
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
                bfs[0].clear();
                bfs[1].clear();
                return;
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
            int n;
            if( (n = client_channel.read(bfs2)) == -1){
                System.out.println("Server: chiusa la connessione con il client " 
                                        + client_channel.getRemoteAddress());
                key.cancel();
                client_channel.close();
                bfs2.clear();
                return;
            }
            
            thisClientPreviuosData = channelToDataMap.get(client_channel);
            // Invariant: here if channelToDataMap.get(client_channel) != null
            if(thisClientPreviuosData == null){
                return;
            }

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

            requestList.add(new CustomRequest(sel, client_channel, message, key));

            // clear buffer
            if(bfs2 != null)
                bfs2.clear();
            if(bfs != null){
                bfs[0].clear();
                bfs[1].clear();
            }

            
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
            //System.out.printf("Server: ricevuto \n%s\n", message);
            registerOp(sel, client_channel,
                    SelectionKey.OP_READ, ByteBuffer.allocate(BUF_SIZE));
        }
        
    }

    /**
     * Write the attached buffer to the channel related to key 
     *
     * @param key chiave di selezione
     * @throws IOException
     */
    private void answerToClient(Selector sel, SelectionKey key) throws IOException {
        SocketChannel client_channel = (SocketChannel) key.channel();
        ByteBuffer BBAnswer = (ByteBuffer) key.attachment();
        client_channel.write(BBAnswer);
        //System.out.println("Scritto al client");
        if (!BBAnswer.hasRemaining()) {
            BBAnswer.clear();
            registerOp(sel, client_channel, SelectionKey.OP_READ, null);
        }
    }


    /**
     * Register the interest to the read operation on the selector
     *
     * @param selector the selector used by the server
     * @param client_channel client socket channel
     * @throws IOException
     */
    protected void registerOp(Selector selector, SocketChannel client_channel, 
                                        int operation, ByteBuffer attached) throws IOException{
        if(attached == null){ 
            /** Invariant: here if operation == OP_READ and channelToDataMap.get(client_channel) == null */
            // create the buffer
            ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
            ByteBuffer message = ByteBuffer.allocate(BUF_SIZE);
            ByteBuffer[] bfs = {length, message};
            // add the client channel to the selector (OP_READ operation is registered)
            // and adds bytebuffer array [length, message] as attachment
            client_channel.register(selector, operation, bfs);
        }else{
            client_channel.register(selector, operation, attached);
        }
    }

    private void startWorkers(){
        for (int i = 0; i < workersAmount; i++) {
			Thread t = new Thread(new NIOWorker(this, requestList, social));
			workers.add(t);
			t.start();
		}
    }
    private void joinWorkers() {
        for (int i = 0; i < workersAmount; i++){
			try {
				workers.get(i).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			};
		}
    }

}