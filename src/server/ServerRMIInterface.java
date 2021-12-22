package server;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import client.ClientNotifyEventInterface;
import exceptions.*;

public interface ServerRMIInterface extends Remote{
    void register(String username, String password, String[] tags) 
                throws RemoteException, TooManyTagsException,
                        InvalidUsername, InvalidTags,
                        NoSuchAlgorithmException, InvalidKeySpecException;

    public void registerForCallback(ClientNotifyEventInterface cInt, String username, String password) 
                throws RemoteException, InvalidUsername, 
                        UsernameAndPasswordMatchException, AlreadyConnectedException, 
                        NoSuchAlgorithmException, InvalidKeySpecException;
    public void unregisterForCallback(ClientNotifyEventInterface cInt) 
                throws RemoteException;
    
}
