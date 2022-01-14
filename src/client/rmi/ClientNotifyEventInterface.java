package client.rmi;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashSet;

public interface ClientNotifyEventInterface extends Remote{
    public void newFollowersList(HashSet<String> hashSet) throws RemoteException, Exception;
}