package database;

import java.util.Set;

import exceptions.DatabaseException;
import social.User;

public interface Database {
    public User getUser(String username);
    public User getAllowedUser(String username, String password) throws DatabaseException;
    public User addNewUser(String username, String password, String[] tags)  throws DatabaseException;
    
    public Set<String> getTags();
    public boolean areTagsValid(String[] tags);
}
