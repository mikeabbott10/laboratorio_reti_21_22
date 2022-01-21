package server.nio;

import java.io.IOException;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.LinkedBlockingQueue;

import com.fasterxml.jackson.core.JsonProcessingException;

import database.Database;
import exceptions.DatabaseException;
import server.ServerMain;
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
        while(!ServerMain.quit){
            CustomRequest req = null;
            try {
                req = requestList.take();
                //LOGGER.info(Thread.currentThread().getName() + " -> " + req);
                HttpResponse response = null;
                try {
                    response = requestHandler.handleRequest(this.db, req);
                } catch (JsonProcessingException | ProtocolException e) {
                    //e.printStackTrace();
                    continue;
                }
                if(response == null) continue;
                // answer to client
                HttpResponseBuilder rb = new HttpResponseBuilder();
                ByteBuffer headers = ByteBuffer.wrap(rb.buildHeaders(response));
                ByteBuffer content = rb.buildContent(response);
                //headers.flip(); content.flip(); flip errati qua perchè ByteBuffer.wrap lascia position a 0
                ByteBuffer bb = ByteBuffer.allocate(headers.capacity() + content.capacity());
                bb.put(headers).put(content);
                bb.flip();
                
                // tell selector thread to register a write operation for channel
                nioServer.pendingRegistrations.add(
                    new RegistrationParameters(req.getSelector(), req.getClient_channel(), SelectionKey.OP_WRITE, bb)
                );
                var junk = ByteBuffer.allocateDirect(1);
                while(nioServer.getRegistrationPipe().sink().write(junk)==0);
                
            } catch (IOException e) {
                // GRAVE: registration pipe issue
                e.printStackTrace();
                return;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }catch (DatabaseException e) {
                // GRAVE
                LOGGER.warn(e.getMessage());
                return;
            }
        }
    }

}