package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.LinkedBlockingQueue;

import social.SocialService;

public class NIOWorker  implements Runnable{
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
            try {
                CustomRequest req = requestList.take();
                System.out.println(Thread.currentThread().getName() + " -> " + req);
                handleRequest(req);
                
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        
    }


    private void handleRequest(CustomRequest req) throws IOException {
        nioServer.registerOp(req.selector, req.client_channel,
                    SelectionKey.OP_WRITE, ByteBuffer.wrap("MESSAGGIO PER IL CLIENT".getBytes()));
        req.selector.wakeup();
    }

}
