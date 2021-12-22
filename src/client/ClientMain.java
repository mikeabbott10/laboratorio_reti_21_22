package client;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.*;
import java.rmi.server.*;

import server.ServerRMIInterface;

public class ClientMain {
    public static final int BUF_SIZE = 4096;

    private static final int SERVER_RMI_PORT = 25258;
    private static final int SERVER_TCP_PORT = 25268;
    private static final String SERVER_IP = "localhost";
    private static final String serverUrl = "rmi://"+ SERVER_IP +":" + SERVER_RMI_PORT;
    private static final String rmiServiceName = "/winsomeservice";

    public static void main(String args[]){

        // RMI
        try {

            // System.out.println("Looking for server");
            ServerRMIInterface server = (ServerRMIInterface) Naming.lookup(serverUrl + rmiServiceName);

            // System.out.println("Registering for callback");
            ClientNotifyEventInterface callbackObj = new ClientNotifyEventImplementation();
            ClientNotifyEventInterface stub = 
                (ClientNotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);

            server.register("nomeUtente", "passwordUtente", new String[]{"primoTag","secondoTag"});
            server.registerForCallback(stub, "nomeUtente", "passwordUtente");
            
            // attende gli eventi generati dal server per
            // un certo intervallo di tempo;
            //Thread.sleep (10000);

            // System.out.println("Unregistering for callback");
            server.unregisterForCallback(stub);
        } catch (Exception e){ 
            e.printStackTrace();
            System.err.println("Client exception:"+ e.getMessage());
        }


        //TCP
        try ( SocketChannel client = SocketChannel.open(new InetSocketAddress("localhost", SERVER_TCP_PORT)); )
        {
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Client: connesso");
            System.out.println("Digita 'exit' per uscire, i messaggi scritti saranno inviati al server:");

            while (true) {

                String msg = consoleReader.readLine().trim();

                if (msg.equals("exit")){
                    break;
                }

                // Creo il messaggio da inviare al server

                // la prima parte del messaggio contiene la lunghezza del messaggio
                ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
                length.putInt(msg.length());
                length.flip();
                client.write(length);
                length.clear();

                // la seconda parte del messaggio contiene il messaggio da inviare
                ByteBuffer readBuffer = ByteBuffer.wrap(msg.getBytes());

                client.write(readBuffer);
                readBuffer.clear();

                /*if (msg.equals(this.EXIT_CMD)){
                    this.exit = true;
                    continue;
                }*/
                ByteBuffer reply = ByteBuffer.allocate(BUF_SIZE);
                client.read(reply);
                reply.flip();

                System.out.printf("Client: il server ha inviato %s\n", new String(reply.array()).trim());
                reply.clear();

            }
            System.out.println("Client: chiusura");
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

}
