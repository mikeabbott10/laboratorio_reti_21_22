package server.rmi;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import client.ClientNotifyEventInterface;
import exceptions.*;
import social.*;

public class ServerRMIImplementation extends RemoteServer implements ServerRMIInterface {
    private SocialService social;

    /**
     * Active users at the moment. Note that we allow users 
     * to be logged from multiple hosts at the same time
     */
    private ConcurrentHashMap<User, Set<ClientNotifyEventInterface>> notifyEnabledClientInterfaces;

    /**
     * Reverse logged user map in order to get faster to the ClientNotifyEventInterface inside notifyEnabledClientInterfaces.
     * Useful for unregistering clients for callbacks in ServerRMIImplementation
     */
    private ConcurrentHashMap<ClientNotifyEventInterface, User> reverseNotifyEnabledClientInterfaces;


    // Constructors ---------------------------------------------------------------------------------------
    public ServerRMIImplementation (SocialService social) {
        super();
        this.social = social;
        this.notifyEnabledClientInterfaces = new ConcurrentHashMap<>();
        this.reverseNotifyEnabledClientInterfaces = new ConcurrentHashMap<>();
    }

    @Override public void register(String username, String password, String[] tags) 
                                throws RemoteException, InvalidUsername, TooManyTagsException, InvalidTags,
                                        NoSuchAlgorithmException, InvalidKeySpecException {
        if(username == null || password == null || tags == null) throw new NullPointerException();
        
        // tags check
        if(tags.length > 5) throw new TooManyTagsException();
        if(!social.areTagsValid(tags)) throw new InvalidTags();
        
        if( social.addNewUser(username, password, tags) != null ) // critical zone. Solved by ConcurrentHashMap
            throw new InvalidUsername("This name already exists.");

        // debug
        System.out.println("DEBUG New User registered: " + username + " " + password + " " + tags);

    }

    /** 
     * Add a new client to the logged users list. Client interface and user are linked together, we don't
     * allow to add, to this list, clients without an identity in the social network.
     * @throws InvalidUsername thrown from SocialService function getAllowedUser
     * @throws UsernameAndPasswordMatchException thrown from SocialService function getAllowedUser
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     * @throws AlreadyConnectedException
     */
    @Override 
    public synchronized void registerForCallback(ClientNotifyEventInterface clientInterface, 
                                            String username, String password) 
                                        throws RemoteException, AlreadyConnectedException,
                                            InvalidUsername, UsernameAndPasswordMatchException,
                                            NoSuchAlgorithmException, InvalidKeySpecException{
        if(username == null || password == null) throw new NullPointerException();

        User user = social.getAllowedUser(username, password);
        // user allowed
        if(!notifyEnabledClientInterfaces.containsKey(user)) {
            // add new (concurrent) set in notifyEnabledClientInterfaces map
            notifyEnabledClientInterfaces.put(user, ConcurrentHashMap.newKeySet() );
        }
        // add new interface in this set
        Set<ClientNotifyEventInterface> cneiSet = notifyEnabledClientInterfaces.get(user);
        if( cneiSet!=null && !cneiSet.add(clientInterface) ){
            // interface was already inside the set
            // note that reverseNotifyEnabledClientInterfaces and 
            // notifyEnabledClientInterfaces are in a consistent state
            throw new AlreadyConnectedException();
        }
        // add new user in reverseNotifyEnabledClientInterfaces map
        reverseNotifyEnabledClientInterfaces.put(clientInterface, user); 
    }

    /* rimuovi registrazione per callback */
    @Override 
    public synchronized void unregisterForCallback(ClientNotifyEventInterface clientInterface)  
                                                    throws RemoteException {
        User user = reverseNotifyEnabledClientInterfaces.remove(clientInterface);
        Set<ClientNotifyEventInterface> cneiSet = notifyEnabledClientInterfaces.get(user);
        if ( cneiSet!=null && cneiSet.remove(clientInterface) )
            System.out.println("Client unregistered");
        else
            System.out.println("Unable to unregister client");
    }

    /*
     * notifica di una variazione di valore dell'azione
     * /* quando viene richiamato, fa il callback a tutti i client
     * registrati
     */
    public synchronized void updateFollowersList() throws RemoteException {
        System.out.println("Starting callbacks.");
        String[] mock = {"test", "test2"};
        Collection<Set<ClientNotifyEventInterface>> coll = notifyEnabledClientInterfaces.values();
        Iterator<Set<ClientNotifyEventInterface>> i = coll.iterator();
        while (i.hasNext()) { // foreach set of ClientNotifyEventInterface
            Iterator<ClientNotifyEventInterface> clientsIterator = i.next().iterator();
            while (clientsIterator.hasNext()) { // foreach ClientNotifyEventInterface in this set
                ClientNotifyEventInterface client = clientsIterator.next();
                try {
                    client.newFollowersList(mock);
                } catch (Exception e) {
                    clientsIterator.remove();
                    e.printStackTrace();
                }
            }
            
        }
        System.out.println("Callbacks complete.");
    }
}