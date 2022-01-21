package client.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;

import client.Client;
import client.ClientMain;
import server.util.Constants;

public class MulticastNotificationReceiver implements Runnable{
    MulticastSocket multicastSocket;
    private InetSocketAddress group;
    private NetworkInterface netInt;
    private int port;
    private Client client;

    public MulticastNotificationReceiver(Client client, int port, InetSocketAddress group, NetworkInterface netInt){
        this.client = client;
        this.port = port;
        this.group = group;
        this.netInt = netInt;
    }

    @Override
    public void run() {
        try {
            this.multicastSocket = new MulticastSocket(port);
            this.multicastSocket.joinGroup(group, netInt);
            this.multicastSocket.setSoTimeout(2000);
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }
        byte[] buffer = new byte[Constants.SOCKET_READ_BUFFER_SIZE_BYTES];
        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
        while (!ClientMain.quit && !Thread.currentThread().isInterrupted()) {
            try{
                this.multicastSocket.receive(dp); // blocking
                //System.out.println("Received: "+ dp.getData());
            }catch(SocketTimeoutException e){
                //System.out.println("udp timed out");
                continue;
            }catch (IOException e) {
                e.printStackTrace();
                break;
            }
            
            dp = new DatagramPacket(buffer, buffer.length); // for next iteration
            
            if (Thread.currentThread().isInterrupted())
                break;
            if(client.printMulticastNotification)
                System.out.println(new String(dp.getData()));
        }

        try {
            this.multicastSocket.leaveGroup(group, netInt);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.multicastSocket.close();
        this.multicastSocket = null;
        System.out.println("Closing...");
    }
    
}
