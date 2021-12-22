package server;

import java.net.Socket;

public class IncomingRequestsHandler  implements Runnable{
    Socket client;

    public IncomingRequestsHandler(Socket client) {
        this.client = client; 
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        
    }
}
