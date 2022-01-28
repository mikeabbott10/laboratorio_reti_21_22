package server.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;

import database.Database;
import exceptions.DatabaseException;
import exceptions.EndOfStreamException;
import server.ServerMain;
import server.http.handler.HttpRequestHandler;
import server.http.response.HttpResponse;
import server.http.response.HttpResponseBuilder;

public class NIOServer {    
    private final int port;
    public final int BUF_SIZE = 8192;

    private Selector selector;
    private Pipe registrationPipe;
    private Database db;

    private final int workersAmount = 4;
    private ThreadPoolExecutor workersPool;

    protected ConcurrentLinkedQueue<RegistrationParameters> pendingRegistrations;
    protected ConcurrentHashMap<SocketChannel, ChannelData> channelToDataMap;

    public boolean end = false;

    public NIOServer(int port, Database db) {
        this.port = port;
        this.db = db;
        this.pendingRegistrations = new ConcurrentLinkedQueue<>();
        this.channelToDataMap = new ConcurrentHashMap<>();
    }

    public Selector getSelector() {
        return selector;
    }
    public Pipe getRegistrationPipe() {
        return registrationPipe;
    }

    /**
     * Start selector cycle
     */
    public void start(){
        workersPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(workersAmount);
        try( 
            ServerSocketChannel serverChannel = ServerSocketChannel.open() 
        ){
            serverChannel.socket().bind( new InetSocketAddress(port) );
            serverChannel.configureBlocking(false); // channel in non blocking mode
            selector = Selector.open();
            // add the server channel to the selector (OP_ACCEPT operation is registered)
            serverChannel.register(selector, SelectionKey.OP_ACCEPT); 

            registrationPipe = Pipe.open();
            registrationPipe.source().configureBlocking(false);
            // add the pipe channel to the selector (OP_READ operation is registered)
            registrationPipe.source().register(selector, SelectionKey.OP_READ);

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
                        if (key.channel() == registrationPipe.source()){
                            var junk = ByteBuffer.allocateDirect(1);
                            registrationPipe.source().read(junk);
                            RegistrationParameters rp = pendingRegistrations.remove();
                            registerOp(rp.selector, rp.clientChannel, rp.operation, rp.bb);
                            junk.clear();
                        }else if (key.isAcceptable()) { // key channel is ready for being accepted
                            ServerSocketChannel server = (ServerSocketChannel) key.channel();
                            SocketChannel client = server.accept(); // non blocking!
                            client.configureBlocking(false);
    
                            // register the SocketChannel to the selector with a
                            // focus on the read operation (we want to read something now) 
                            registerOp(selector, client, SelectionKey.OP_READ, null);
                        }
                        else if (key.isWritable()) { // key channel is ready for being written
                            answerToClient(selector, key);
                        }else if (key.isReadable()) { // key channel is ready for being read
                            readClientMessage(selector, key);
                        }
                    }catch (IOException ex) { // client suddenly closed
                        //ex.printStackTrace();
                        cancelKeyAndCloseChannel(key);
                    }
                }
            }

            // close pipe channels
            registrationPipe.source().close();
            registrationPipe.sink().close();
        }catch (IOException ex) {
            ex.printStackTrace();
        }

        try {
            workersPool.shutdown();
            workersPool.awaitTermination(4, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        };

        synchronized(this){
            end = true;
            this.notify();
        }
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
            // pre parse to wait for whole request (post and put with body come here twice)
            int contentLength;
            if((contentLength = containsContentLength(raw)) == -2){
                // it's the body
                ChannelData prevData = channelToDataMap.get(clientChannel);
                if(prevData!=null){
                    channelToDataMap.remove(clientChannel);
                    this.workersPool.execute( this.requestHandlerTask(new CustomRequest(sel, clientChannel, prevData.getMessage() + raw, key)) );
                    //requestList.add(new CustomRequest(clientChannel, prevData.getMessage() + raw, key));
                }else{
                    // never here
                    assert true == false;
                }
            }else if(contentLength == -1){
                // it's an http request containing no body (it contains method, path, http version and headers)
                channelToDataMap.remove(clientChannel);
                this.workersPool.execute( this.requestHandlerTask(new CustomRequest(sel, clientChannel, raw, key)) );
                //requestList.add(new CustomRequest(clientChannel, raw, key));
            }else{
                // System.out.println("NIOServer.readClientMessage - contentLength:  "+contentLength);
                // it's an http request which should contain body
                if(containsBody(raw, contentLength)){
                    //System.out.println("contiene body");
                    channelToDataMap.remove(clientChannel);
                    this.workersPool.execute( this.requestHandlerTask(new CustomRequest(sel, clientChannel, raw, key)) );
                    //requestList.add(new CustomRequest(clientChannel, raw, key));
                }else{
                    //System.out.println("NON contiene body");
                    ChannelData cd = new ChannelData();
                    cd.setMessage(raw);
                    cd.setMissingBytes(contentLength);
                    channelToDataMap.put(clientChannel, cd);
                    //System.out.printf("Server: ricevuto \n%s\n", message);
                    registerOp(sel, clientChannel, SelectionKey.OP_READ, null);
                }

            }
        }catch(EndOfStreamException e){
            //System.out.println(e.getMessage() +": "+ key.channel());
            cancelKeyAndCloseChannel(key);
        } catch (IOException e) {
            //System.out.println(e.getMessage());
            cancelKeyAndCloseChannel(key);
        }
    }

    /**
     * Check if http request raw contains body
     * @param raw
     * @param contentLength
     * @return
     */
    private boolean containsBody(String raw, int contentLength) {
        final String eol = "\r\n\r\n" ;
        try {
            String body = raw.split(eol)[1];
            //System.out.println("NIOServer.containsBody - body.length(): "+ body.length());
            //System.out.println("NIOServer.containsBody - body: "+ body);
            if(body.length() == contentLength)
                return true;
        }catch(Exception e){
            //e.printStackTrace();
        }
        return false;
    }

    /**
     * check if http request raw contains Content-Length header > 0
     * @param raw
     * @return
     */
    private int containsContentLength(String raw) {
        //System.out.println("NIOServer.containsContentLength - request: "+raw);
        try {
            String[] rows = raw.split("\n");
            //System.out.println("NIOServer.containsContentLength - 1");
            if(rows.length <= 2) // this is the body of the req
                return -2;
            //System.out.println("NIOServer.containsContentLength - 2");
            for (String row : rows) {
                //System.out.println("NIOServer.containsContentLength - "+ row);
                if(row.equals("\r")) return -1; // eol without \n
                String[] arr = row.split(":");
                String key = new String(arr[0].trim());
                //System.out.println("NIOServer.containsContentLength - key: "+key);
                if(key.equals("Content-Length")){
                    if(new String(arr[1].trim()).equals("0"))
                        return -1;
                    return Integer.parseInt(new String(arr[1].trim()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
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
     *  NOTE:
     *      Selectable channels are safe for use by multiple concurrent threads.
     *  NOTE:
     *      A Selector and its key set are safe for use by multiple concurrent threads. 
     *      Its selected-key set and cancelled-key set, however, are not.
     * 
     * channel.register adds a key in the set of keys representing 
     * the current channel registrations of the selector.
     * 
     * @param selector the selector used by the server
     * @param client_channel client socket channel
     * @throws IOException
     */
    protected void registerOp(Selector selector, SocketChannel client_channel, 
                                int operation, ByteBuffer attached) throws IOException{
        try{
            if(attached == null){
                client_channel.register(selector, operation);
            }else{
                client_channel.register(selector, operation, attached);
            }
        }catch(ClosedChannelException e){}
    }

    protected void cancelKeyAndCloseChannel(SelectionKey key) {
        try{
            key.cancel();
            key.channel().close();
        }catch(Exception ignored){}
        //System.out.println("chiuso");
    }

    private Runnable requestHandlerTask(CustomRequest req){
        return() -> {
            try {
                //System.out.println(Thread.currentThread().getName());
                HttpResponse response = null;
                try {
                    response = new HttpRequestHandler().handleRequest(this.db, req);
                } catch (JsonProcessingException | ProtocolException e) {
                    e.printStackTrace();
                    return;
                }
                if(response == null) return;
                // answer to client
                HttpResponseBuilder rb = new HttpResponseBuilder();
                ByteBuffer headers = ByteBuffer.wrap(rb.buildHeaders(response));
                ByteBuffer content = rb.buildContent(response);
                //headers.flip(); content.flip(); flip errati qua perchè ByteBuffer.wrap lascia position a 0
                ByteBuffer bb = ByteBuffer.allocate(headers.capacity() + content.capacity());
                bb.put(headers).put(content);
                bb.flip();
                
                // tell selector thread to register a write operation for channel
                this.pendingRegistrations.add(
                    new RegistrationParameters(req.getSelector(), req.getClient_channel(), SelectionKey.OP_WRITE, bb)
                );
                var junk = ByteBuffer.allocateDirect(1);
                while(this.getRegistrationPipe().sink().write(junk)==0);
                
            } catch (IOException e) {
                // GRAVE: registration pipe issue
                e.printStackTrace();
            }catch (DatabaseException e) {
                // GRAVE
                System.out.println("Warn: "+e.getMessage());
            }
        };
    }
}