package client.rmi;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.Set;

import client.Client;

public class ClientNotifyEventImplementation extends RemoteObject 
                                    implements ClientNotifyEventInterface {
        private Client client;

        public ClientNotifyEventImplementation(Client client) throws RemoteException { 
            super();
            this.client = client;
        }

        /**
        * Called by server to notify the new followers list
        * @throws RemoteException
        */
        @Override
        public void newFollowersList(Set<String> updatedFollowers) 
                    throws RemoteException {
            client.followers = updatedFollowers;
        }

        @Override
        public void noMoreLoggedNotification() throws RemoteException {
            this.client.logoutNotification.set(true);
            
        }
    }