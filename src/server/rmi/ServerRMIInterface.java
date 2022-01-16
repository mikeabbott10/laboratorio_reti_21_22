package server.rmi;
import java.rmi.Remote;
import java.rmi.RemoteException;

import client.rmi.ClientNotifyEventInterface;
import exceptions.*;

public interface ServerRMIInterface extends Remote{
        
    public String register(String username, String password, String[] tags) 
                throws RemoteException, TooManyTagsException,
                        InvalidUsername, DatabaseException;

    public void registerForCallback(ClientNotifyEventInterface cInt, String username, String password) 
                throws RemoteException, AlreadyConnectedException, DatabaseException, LoginException;
    public void unregisterForCallback(ClientNotifyEventInterface cInt) 
                throws RemoteException;
    
}
