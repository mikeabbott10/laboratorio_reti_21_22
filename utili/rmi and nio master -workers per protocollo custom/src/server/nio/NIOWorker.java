package server.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.LinkedBlockingQueue;

import server.util.Logger;
import social.SocialService;

public class NIOWorker  implements Runnable{
    private static final Logger LOGGER = new Logger(NIOWorker.class.getName());

    private LinkedBlockingQueue<CustomRequest> requestList;
    private SocialService social;
    private NIOServer nioServer;

    public NIOWorker(NIOServer nioServer, LinkedBlockingQueue<CustomRequest> requestList, SocialService social) {
        this.nioServer = nioServer;
        this.requestList = requestList;
        this.social = social;
    }

    @Override
    public void run() {
        while(true){
            CustomRequest req = null;
            try {
                req = requestList.take();
                LOGGER.info(Thread.currentThread().getName() + " -> " + req);
                handleRequest(req);
            } catch (IOException ioe) {
                LOGGER.info(ioe.getMessage());
                try{
                    req.getKey().cancel();
                    req.getClient_channel().close();
                }catch(Exception ignored){}
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            
            }
        }
        
    }


    private void handleRequest(CustomRequest req) throws IOException {
        // TODO: handle req
        // answer to client
        nioServer.registerOp(req.getSelector(), req.getClient_channel(),
                    SelectionKey.OP_WRITE, ByteBuffer.wrap("MESSAGGIO PER IL CLIENT".getBytes()));
        req.getSelector().wakeup();
    }

}
