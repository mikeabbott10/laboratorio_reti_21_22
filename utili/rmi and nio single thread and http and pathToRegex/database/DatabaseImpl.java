package database;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Set;

import exceptions.DatabaseException;
import social.*;

/**
 * The database implementation. It's just a simulator.
 * This is NOT a real relational db implementation.
 */
public class DatabaseImpl implements Database{
    private SocialService social;

    public DatabaseImpl(SocialService social){
        this.social= social;
    }

    // DB BACKEND -----------------------------------------------------------------------
    //#region Backend
    /**
     * query simulation:
     *      SELECT * FROM users U WHERE U.Username=username
     * @param username
     * @return the user in the db or null
     */
    @Override
    public User getUser(String username){
        return social.getUsers().get(username);
    }

    /**
     * query simulation:
     *      SELECT * FROM users U WHERE U.Username=username AND U.Password=password
     * @param username 
     * @param password
     * @return the user in the db or null
     * @throws DatabaseException grave exception in db
     */
    @Override
    public User getAllowedUser(String username, String password) throws DatabaseException{
        User user = social.getUsers().get(username);
        if(user==null) 
            return null; // throw new InvalidUsername("This name does not exists.");
        try {
            if(!user.getPassword().passwordMatches(password)) 
                return null; // throw new UsernameAndPasswordMatchException();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new DatabaseException(e.getMessage());
        }
        return user;
    }

    /**
     * query simulation:
     *      INSERT INTO users (Username, Password, Tags) VALUES (username, password, tags);
     * @param username
     * @param password
     * @param tags
     * @return the new user in the db
     * @throws DatabaseException grave exception in db
     */
    @Override
    public User addNewUser(String username, String password, String[] tags) 
            throws DatabaseException{
        try {
            return social.getUsers().putIfAbsent(username, new User(social, username, password, tags));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    /**
     * query simulation:
     *      SELECT * FROM tags
     * @return tags in the db
     */
    @Override
    public Set<String> getTags() {
        return social.getTags();
    }
    //#endregion


    // MIDDLEWARE server / db -------------------------------------------------------------
    //#region middleware
    /**
     * Say if tags are allowed
     * @param tags
     * @return true if tags are allowed
     */
    @Override
    public boolean areTagsValid(String[] tags) {
        for (String tag : tags) {
            if( ! social.getTags().contains(tag) )
                return false;
        }
        return true;
    }
    //#endregion
    
}
