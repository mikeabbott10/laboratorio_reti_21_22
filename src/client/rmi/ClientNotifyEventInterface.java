package client.rmi;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface ClientNotifyEventInterface extends Remote{
    public void newFollowersList(Set<String> set) throws RemoteException;
    public void noMoreLoggedNotification() throws RemoteException;
}