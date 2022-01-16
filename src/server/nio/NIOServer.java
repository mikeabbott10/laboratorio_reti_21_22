package server.nio;

import java.nio.*; 
import java.nio.channels.*;
import java.net.*; 
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import database.Database;
import exceptions.DatabaseException;
import exceptions.EndOfStreamException;
import server.ServerMain;
import server.http.handler.HttpRequestHandler;
import server.http.response.HttpResponse;
import server.http.response.HttpResponseBuilder;
import server.util.Logger;

import java.io.IOException;

public class NIOServer {
    private static final Logger LOGGER = new Logger(NIOServer.class.getName());
    
    private final int port;
    public final int BUF_SIZE = 4096;

    private Selector selector;
    private Database db;

    private final int workersAmount = 4;
    private ArrayList<Thread> workers;

    protected LinkedBlockingQueue<CustomRequest> requestList;

    public NIOServer(int port, Database db) {
        this.port = port;
        this.db = db;
        this.requestList = new LinkedBlockingQueue<>();
        this.workers = new ArrayList<>(workersAmount);
    }

    public Selector getSelector() {
        return selector;
    }


    public void start(){
        startWorkers();
        try( 
            ServerSocketChannel serverChannel = ServerSocketChannel.open() 
        ){
            serverChannel.socket().bind( new InetSocketAddress(port) );
            serverChannel.configureBlocking(false); // channel in non blocking mode
            selector = Selector.open();
            // add the server channel to the selector (OP_ACCEPT operation is registered)
            serverChannel.register(selector, SelectionKey.OP_ACCEPT); 

            while (!ServerMain.quit) {
                if( selector.select() == 0 )
                    continue;
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
                        cancelKeyAndCloseChannel(key);
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
    private void readClientMessage(Selector sel, SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        try{
            String raw = new RawRequestReader().readRaw(clientChannel);
            requestList.add(new CustomRequest(sel, clientChannel, raw, key));
        }catch(EndOfStreamException e){
            cancelKeyAndCloseChannel(key);
        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
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
        //LOGGER.info("Scritto al client");
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

    protected void cancelKeyAndCloseChannel(SelectionKey key) {
        try{
            key.cancel();
            key.channel().close();
        }catch(Exception ignored){}
        //LOGGER.info("chiuso");
    }

    private void startWorkers(){
        for (int i = 0; i < workersAmount; i++) {
			Thread t = new Thread(new NIOWorker(this, requestList, this.db));
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