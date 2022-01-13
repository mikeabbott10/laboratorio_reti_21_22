package client;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.HashSet;

public class ClientNotifyEventImplementation extends RemoteObject 
                            implements ClientNotifyEventInterface {
    public ClientNotifyEventImplementation() throws RemoteException { 
        super(); 
    }

    /**
     * Called by server to notify the new followers list
     * @throws Exception
     */
    @Override
    public void newFollowersList(HashSet<String> followersList) 
            throws RemoteException, Exception {
        System.out.println("new followers list received: "+ followersList);
        
    }

    
}