package server.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.LinkedBlockingQueue;

import database.Database;
import exceptions.DatabaseException;
import server.http.handler.HttpRequestHandler;
import server.http.response.HttpResponse;
import server.http.response.HttpResponseBuilder;
import server.util.Logger;

public class NIOWorker  implements Runnable{
    private static final Logger LOGGER = new Logger(NIOWorker.class.getName());

    private LinkedBlockingQueue<CustomRequest> requestList;
    private NIOServer nioServer;
    private HttpRequestHandler requestHandler;

    private Database db;

    public NIOWorker(NIOServer nioServer, LinkedBlockingQueue<CustomRequest> requestList, Database db) {
        this.nioServer = nioServer;
        this.requestList = requestList;
        this.requestHandler = new HttpRequestHandler();
        this.db = db;
    }

    @Override
    public void run() {
        while(true){
            CustomRequest req = null;
            try {
                req = requestList.take();
                //LOGGER.info(Thread.currentThread().getName() + " -> " + req);
                HttpResponse response = requestHandler.handleRequest(this.db, req);
                if(response == null) continue;
                // answer to client
                HttpResponseBuilder rb = new HttpResponseBuilder();
                ByteBuffer headers = ByteBuffer.wrap(rb.buildHeaders(response));
                ByteBuffer content = rb.buildContent(response);
                //headers.flip(); content.flip(); flip errati qua perchè ByteBuffer.wrap lascia position a 0
                ByteBuffer bb = ByteBuffer.allocate(headers.capacity() + content.capacity());
                bb.put(headers).put(content); 
                bb.flip();
                nioServer.registerOp(req.getSelector(), req.getClient_channel(),
                                        SelectionKey.OP_WRITE, bb);
                req.getSelector().wakeup();
            }catch (IOException ioe) {
                LOGGER.info(ioe.getMessage());
                nioServer.cancelKeyAndCloseChannel(req.getKey());
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }catch (DatabaseException e) {
                // GRAVE
                LOGGER.warn(e.getMessage());
            }
        }
    }

    protected void cancelKeyAndCloseChannel(SelectionKey key) {
        try{
            key.cancel();
            key.channel().close();
        }catch(Exception ignored){}
        //LOGGER.info("chiuso");
    }
}