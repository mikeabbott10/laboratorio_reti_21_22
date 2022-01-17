package client.rmi;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.HashSet;

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
        * @throws Exception
        */
        @Override
        public void newFollowersList(HashSet<String> updatedFollowers) 
            throws RemoteException, Exception {
            client.followers = updatedFollowers;
        }
    }