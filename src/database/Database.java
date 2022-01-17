package database;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import database.social.Post;
import database.social.User;
import exceptions.AlreadyConnectedException;
import exceptions.DatabaseException;
import exceptions.ForbiddenActionException;
import exceptions.ResourceNotFoundException;

public interface Database {
    public User getUser(String username);
    public User getAllowedUser(String username, String password) throws DatabaseException;
    public User addNewUser(String username, String password, String[] tags) throws DatabaseException;
    
    public int createPost(String title, String content, String author) throws DatabaseException, ForbiddenActionException;
    public void removePost(User user, int postID) throws ResourceNotFoundException;

    public void rewinPost(int postID, String author) throws ResourceNotFoundException, ForbiddenActionException;
    public void addComment(int postID, String comment, String author) throws ResourceNotFoundException, ForbiddenActionException;
    public Post getPost(int postID);
    public Post[] getPostsFromUsername(String string);
    public HashSet<User> getUsersFromTagname(String string);
    public void addFollowerTo(String username, String string) throws ResourceNotFoundException;
    public void removeFollowerTo(String username, String userToFollow) throws ResourceNotFoundException;
    public void addVoteTo(int parseInt, String username) throws ResourceNotFoundException, ForbiddenActionException;
    public void addDownvoteTo(int postID, String username) throws ResourceNotFoundException, ForbiddenActionException;
    public ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<String, Boolean>> getNewUpvotes();
    public ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<String, Boolean>> getNewDownvotes();
    public ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<String, Boolean>> getNewComments();

    public ConcurrentHashMap<Integer, Post> getPosts();
    public ConcurrentHashMap<String, User> getUsers();
    public void removeIdFromNewUpvotes(Integer id);
    public void removeIdFromNewDownvotes(Integer id);
    public void removeIdFromNewComments(Integer id);
    public void updateUserWallet(String username, double amountToAdd) throws ResourceNotFoundException;
    public Object getSocialInstance();
	public void addLoggedUser(String username) throws AlreadyConnectedException;
    public void removeLoggedUser(String username);
    public int updateRewardIterations();
    
}
