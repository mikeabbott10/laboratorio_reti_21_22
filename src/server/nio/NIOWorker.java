package server.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.LinkedBlockingQueue;

import server.http.handler.HttpRequestHandler;
import server.http.response.HttpResponse;
import server.http.response.HttpResponseBuilder;
import server.util.Logger;
import social.SocialService;

public class NIOWorker  implements Runnable{
    private static final Logger LOGGER = new Logger(NIOWorker.class.getName());

    private LinkedBlockingQueue<CustomRequest> requestList;
    private SocialService social;
    private NIOServer nioServer;
    private HttpRequestHandler requestHandler;

    public NIOWorker(NIOServer nioServer, LinkedBlockingQueue<CustomRequest> requestList, SocialService social) {
        this.nioServer = nioServer;
        this.requestList = requestList;
        this.social = social;
        this.requestHandler = new HttpRequestHandler();
    }

    @Override
    public void run() {
        while(true){
            CustomRequest req = null;
            try {
                req = requestList.take();
                //LOGGER.info(Thread.currentThread().getName() + " -> " + req);
                HttpResponse response = requestHandler.handleRequest(req);
                if(response == null) continue;
                // answer to client
                HttpResponseBuilder rb = new HttpResponseBuilder();
                ByteBuffer headers = ByteBuffer.wrap(rb.buildHeaders(req.getClient_channel(), response));
                ByteBuffer content = rb.buildContent(req.getClient_channel(), response);
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
            
            }
        }
        
    }

    /*private void handleRequest(CustomRequest req) throws IOException {
        
        // answer to client
        nioServer.registerOp(req.selector, req.client_channel,
                    SelectionKey.OP_WRITE, ByteBuffer.wrap("MESSAGGIO PER IL CLIENT".getBytes()));
        req.selector.wakeup();
    }*/

}
