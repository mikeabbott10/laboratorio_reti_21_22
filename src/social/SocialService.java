package social;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ConcurrentHashMap;

import exceptions.InvalidUsername;
import exceptions.UsernameAndPasswordMatchException;


public class SocialService {
    private ConcurrentHashMap<String, User> users; // users in the social net

    // Getters -------------------------------------------------------------------------------------------
    public ConcurrentHashMap<String, User> getUsers() {
        return users;
    }

    // Constructor ---------------------------------------------------------------------------------------
    public SocialService(){
        users = new ConcurrentHashMap<>();
    }

    public User getAllowedUser(String username, String password) 
            throws InvalidUsername, UsernameAndPasswordMatchException,
                    NoSuchAlgorithmException, InvalidKeySpecException{
        User user = users.get(username);
        if(user==null) throw new InvalidUsername("This name does not exists.");
        if(!user.getPassword().passwordMatches(password)) throw new UsernameAndPasswordMatchException();
        return user;
    }

    public User addNewUser(String username, String password, String[] tags) 
                                            throws NoSuchAlgorithmException, InvalidKeySpecException{
        return users.putIfAbsent(username, new User(username, password, tags));
    }

    public boolean areTagsValid(String[] tags) {
        //TODO
        return true;
    }
}
