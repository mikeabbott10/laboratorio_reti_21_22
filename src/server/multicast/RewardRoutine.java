package server.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import cloudfunctions.RewardCalculator;
import database.Database;
import server.ServerMain;
import server.nio.NIOServer;

/**
 * Calculate the rewards with this routine
 */
public class RewardRoutine implements Runnable{
    private Database db;
    private NIOServer nio;

    public RewardRoutine(NIOServer nio, Database db){
        this.nio= nio;
        this.db = db;
    }

    @Override
    public void run() {
        try (DatagramSocket skt = new DatagramSocket(ServerMain.server_config.MULTICAST_PORT +1 )) {
            while (!Thread.currentThread().isInterrupted() && !ServerMain.quit ) {
                byte[] msg = "Rewards calculated".getBytes();
                try {
                    DatagramPacket datagram = new DatagramPacket(msg, msg.length,
                        InetAddress.getByName(ServerMain.server_config.MULTICAST_ADDRESS),
                        ServerMain.server_config.MULTICAST_PORT);
                    skt.send(datagram);
                } catch (UnknownHostException | SocketException e) {
                    // packet or socket error
                    e.printStackTrace();
                } catch (IOException ex) {
                    // comunication error
                    ex.printStackTrace();
                }
                RewardCalculator.calculate(db);
                try {
                    Thread.sleep(ServerMain.server_config.REWARD_TIMEOUT);
                } catch (InterruptedException ignored){}
            }

            // wait until nio workers end
            synchronized (nio){
                while (!nio.end) {
                    try {
                        nio.wait();
                    }catch (InterruptedException ex){
                        ex.printStackTrace();
                    }
                }
            }
            //System.out.println("Performing last reward calculation");
            RewardCalculator.calculate(db); // once more after all stopped
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return;
    }

    
}
