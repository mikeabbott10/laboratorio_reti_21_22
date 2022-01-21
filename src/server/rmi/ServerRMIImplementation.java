package server.rmi;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ConcurrentHashMap;

import client.rmi.ClientNotifyEventInterface;
import database.Database;
import database.social.*;
import exceptions.*;

public class ServerRMIImplementation extends RemoteServer implements ServerRMIInterface {
    private final Database db;

    /**
     * Active users at the moment.
     */
    private ConcurrentHashMap<String, ClientNotifyEventInterface> notifyEnabledClientInterfaces;

    /**
     * Reverse logged user map in order to get faster to the ClientNotifyEventInterface inside notifyEnabledClientInterfaces.
     * Useful for unregistering clients for callbacks in ServerRMIImplementation
     */
    private ConcurrentHashMap<ClientNotifyEventInterface, String> reverseNotifyEnabledClientInterfaces;


    // Constructors ---------------------------------------------------------------------------------------
    public ServerRMIImplementation (Database db) {
        super();
        this.db = db;
        this.notifyEnabledClientInterfaces = new ConcurrentHashMap<>();
        this.reverseNotifyEnabledClientInterfaces = new ConcurrentHashMap<>();
    }

    @Override 
    public String register(String username, String password, String[] tags) 
                                throws RemoteException, InvalidUsername, 
                                        TooManyTagsException, DatabaseException{
        if(username == null || password == null || tags == null) throw new NullPointerException();

        // tags check
        if(tags.length > 5) throw new TooManyTagsException();

        if( db.addNewUser(username.trim(), password, tags) != null ) // critical zone. Solved by ConcurrentHashMap
            throw new InvalidUsername("This name already exists.");

        //System.out.println("New User registered: " + username + " " + password + " " + tags);
        return "User "+ username + "registered.";
    }

    /** 
     * Add a new client to the logged users list. Client interface and user are linked together, we don't
     * allow to add, to this list, clients without an identity in the social network.
     * @throws InvalidUsername thrown from db.getAllowedUser
     * @throws UsernameAndPasswordMatchException 
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     * @throws AlreadyConnectedException
     * @throws LoginException
     */
    @Override 
    public synchronized void registerForCallback(ClientNotifyEventInterface clientInterface, 
                                                    String username, String password) 
                                        throws RemoteException, AlreadyConnectedException,
                                                LoginException, DatabaseException{
        /*User user = */getAllowedUser(username, password); // throws LoginException, DatabaseException

        // user allowed
        // add new interface
        if(!notifyEnabledClientInterfaces.containsKey(username)) {
            notifyEnabledClientInterfaces.put(username, clientInterface );
            // add new user in reverseNotifyEnabledClientInterfaces map
            reverseNotifyEnabledClientInterfaces.put(clientInterface, username);
        }else{
            throw new AlreadyConnectedException();
        }
    }

    private User getAllowedUser(String username, String password) 
                                        throws DatabaseException, LoginException {
        if(username == null || password == null) throw new NullPointerException();
        User user = db.getAllowedUser(username, password);
        if(user == null) throw new LoginException();
        return user;
    }

    /* rimuovi registrazione per callback */
    @Override 
    public synchronized void unregisterForCallback(ClientNotifyEventInterface clientInterface)  
                                                    throws RemoteException {
        String username = reverseNotifyEnabledClientInterfaces.remove(clientInterface);
        if(username == null){
            System.out.println("Unable to unregister client");
            return;
        }
        notifyEnabledClientInterfaces.remove(username);
        System.out.println("Client unregistered");
    }

    public synchronized void safeUnregisterForCallback(String username){
        ClientNotifyEventInterface clientInterface = notifyEnabledClientInterfaces.remove(username);
        if(clientInterface==null){
            System.out.println("Unable to unregister client");
            return;
        }
        reverseNotifyEnabledClientInterfaces.remove(clientInterface);
        System.out.println("Client unregistered");
    }

    /*
     * Send user followers list to user
     */
    public synchronized void updateFollowersList(User user) {
        if(user==null) return;
        ClientNotifyEventInterface clientInterface = notifyEnabledClientInterfaces.get(user.getUsername());
        if(clientInterface==null) return;
        try {
            synchronized(db.getFollowObj()){
                clientInterface.newFollowersList(user.getFollowers());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Send no more logged notification
     * @param uname
     * @throws RemoteException
     */
    public synchronized void sendLogoutNotification(String uname){
        if(uname==null) return;
        ClientNotifyEventInterface clientInterface = notifyEnabledClientInterfaces.get(uname);
        //System.out.println(clientInterface);
        if(clientInterface==null) return;
        try {
            clientInterface.noMoreLoggedNotification();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}