package client.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import client.ClientMain;
import server.util.Constants;

public class MulticastNotificationReceiver implements Runnable{
    MulticastSocket multicastSocket;

    public MulticastNotificationReceiver(MulticastSocket multicastSocket) throws SocketException {
        this.multicastSocket = multicastSocket;
        this.multicastSocket.setSoTimeout(500);
    }

    @Override
    public void run() {
        byte[] buffer = new byte[Constants.SOCKET_READ_BUFFER_SIZE_BYTES];
        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
        while (!ClientMain.quit && !Thread.currentThread().isInterrupted()) {
            try{
                this.multicastSocket.receive(dp); // blocking
            }catch(SocketTimeoutException e){
                continue;
            }catch (IOException e) {
                e.printStackTrace();
                break;
            }
            
            dp = new DatagramPacket(buffer, buffer.length); // for next iteration

            if (Thread.currentThread().isInterrupted())
                break;
            System.out.println(new String(dp.getData()));
        }
        
    }
    
}
