package client;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

public class ClientNotifyEventImplementation extends RemoteObject 
                            implements ClientNotifyEventInterface {
    public ClientNotifyEventImplementation() throws RemoteException { 
        super(); 
    }

    /**
     * metodo che può essere richiamato dal servente per 
     * notificare aggiornamento della lista di followers
     * @throws Exception
     */
    public void newFollowersList (String[] users) throws RemoteException {
        System.out.println("new followers list received");
    }

    
}