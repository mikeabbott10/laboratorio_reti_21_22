package database;

import java.util.HashMap;

import exceptions.DatabaseException;
import social.Post;
import social.User;

public interface Database {
    public User getUser(String username);
    public User getAllowedUser(String username, String password) throws DatabaseException;
    public User addNewUser(String username, String password, String[] tags) throws DatabaseException;
    
    public int createPost(String title, String content, String author) throws DatabaseException;
    public boolean removePost(User user, int postID);

    public void rewinPost(int postId, String author);
    public void addComment(String postId, String comment, String author);
    public Post getPost(int postID);
    public Post[] getPostsFromUsername(String string);
    public HashMap<String, User> getUsersFromTagname(String string);
    
}
