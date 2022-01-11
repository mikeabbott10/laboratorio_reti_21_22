package client;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientNotifyEventInterface extends Remote{
    public void newFollowersList(String[] users) throws RemoteException, Exception;
    //public int compareTo(ClientNotifyEventInterface o) throws RemoteException;
}