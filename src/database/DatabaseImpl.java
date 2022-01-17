package database;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

import database.social.Post;
import database.social.SocialService;
import database.social.User;
import exceptions.AlreadyConnectedException;
import exceptions.DatabaseException;
import exceptions.ForbiddenActionException;
import exceptions.ResourceNotFoundException;

/**
 * The database implementation. It's just a simulator.
 * This is NOT a real relational db implementation.
 */
public class DatabaseImpl implements Database{
    private SocialService social;

    public DatabaseImpl(SocialService social){
        this.social= social;
    }

    //#region
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

    @Override
    public ConcurrentHashMap<String, User> getUsers() {
        return social.getUsers();
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
     *      DELETE FROM posts P WHERE P.id=postID;
     * @param user the author of the post
     * @param postID the post id
     * @throws ResourceNotFoundException
     */
    @Override
    public void removePost(User user, int postID) 
            throws ResourceNotFoundException{
        if( user.getPosts().remove(postID) == false )
            throw new ResourceNotFoundException("Post not found");
        Post p = social.getPosts().remove(postID);
        if( p != null ){
            p.getRewinnedBy().forEach((String username) -> {
                social.getUsers().get(username).getPosts().remove(postID);
            });
            return;
        }
        throw new ResourceNotFoundException("Post not found");
    }

    /**
     * query simulation:
     *      INSERT INTO posts (Title, Content, Author) VALUES (title, content, author);
     * @param title
     * @param content
     * @param author the username of the author
     * @throws DatabaseException
     * @throws ForbiddenActionException
     */
    @Override
    public int createPost(String title, String content, String author) throws DatabaseException, ForbiddenActionException{
        if(content.length()>500 || title.length()>20){
            //System.out.println("Title max length: 20 characters. Content max length: 500 characters.");
            throw new ForbiddenActionException();
        }
        int postId = social.postIdCounter++;
        Post post = new Post(postId, title, content, author, social.getRewardRoutineAge());
        if( social.getUsers().get(author).addPost(postId) == false || 
                social.getPosts().put(postId, post) != null )
            throw new DatabaseException();
        return postId;
    }

    /**
     * query simulation:
     *      SELECT * FROM posts P WHERE P.id=postID
     */
    @Override
    public Post getPost(int postID) {
        return social.getPosts().get(postID);
    }

    /**
     * query simulation:
     *      SELECT * FROM posts P WHERE P.author=username
     */
    @Override
    public Post[] getPostsFromUsername(String username) {
        Vector<Post> posts = new Vector<>();
        User u = social.getUsers().get(username);
        if(u==null)
            return null;
        try{
            for (int postID : u.getPosts()) {
                posts.add( social.getPosts().get(postID) );
            };
        }catch(Exception e){
            return null;
        }

        Object[] objArray = posts.toArray();
        // Converting object array to string array
        Post[] posts_array = Arrays.copyOf(
            objArray, objArray.length, Post[].class);
        return posts_array ;
    }

    /**
     * query simulation:
     *      SELECT * FROM users U WHERE <U.tag contains tagname>
     */
    @Override
    public HashSet<User> getUsersFromTagname(String tagname) {
        HashSet<User> users = new HashSet<>();
        social.getUsers().forEach((username, user) -> {
            for (String tag : user.getTags()) {
                if(tag.equals(tagname) ){
                    users.add(user);
                    break;
                }
            }
        });

        return users;
    }

    @Override
    public void addLoggedUser(String username) throws AlreadyConnectedException{
        if( !social.getLoggedUsers().add(username) ){
            throw new AlreadyConnectedException();
        }
    }

    @Override
    public void removeLoggedUser(String username) {
        social.getLoggedUsers().remove(username);
    }

    /**
     * add 1 to reward routine age.
     */
    @Override
    public int updateRewardIterations() {
        social.setRewardRoutineAge(social.getRewardRoutineAge()+1);
        return social.getRewardRoutineAge();
        
    }

    @Override
    public ConcurrentHashMap<Integer, KeySetView<String, Boolean>> getNewUpvotes() {
        return social.getNewUpvotes();
    }

    @Override
    public ConcurrentHashMap<Integer, KeySetView<String, Boolean>> getNewDownvotes() {
        return social.getNewDownvotes();
    }

    @Override
    public ConcurrentHashMap<Integer, KeySetView<String, Boolean>> getNewComments() {
        return social.getNewComments();
    }

    @Override
    public ConcurrentHashMap<Integer, Post> getPosts() {
        return social.getPosts();
    }

    @Override
    public Object getSocialInstance() {
        return social;
    }

    //#endregion  

    //#region PUT request works

    // /post
    @Override
    public synchronized void addComment(int postID, String comment, String author) 
            throws ResourceNotFoundException, ForbiddenActionException{
        checkPostUserInteractionValidity(postID, author);
        social.getPosts().get(postID).addComment(author, comment);
        social.getNewComments().putIfAbsent(postID, ConcurrentHashMap.newKeySet());
        social.getNewComments().get(postID).add(author);
    }

    @Override
    public synchronized void rewinPost(int postID, String rewinner) 
            throws ResourceNotFoundException, ForbiddenActionException {
        checkPostUserInteractionValidity(postID, rewinner);
        social.getUsers().get(rewinner).getPosts().add(postID);
        social.getPosts().get(postID).getRewinnedBy().add(rewinner);
    }
    
    @Override
    public synchronized void addVoteTo(int postID, String username) 
            throws ResourceNotFoundException, ForbiddenActionException {
        checkPostUserInteractionValidity(postID, username);
        if(social.getPosts().get(postID).getAuthor().equals(username))
            throw new ForbiddenActionException();
        if(social.getNewUpvotes().putIfAbsent(postID, ConcurrentHashMap.newKeySet())!=null)
            throw new ForbiddenActionException("Upvote already inserted");
        social.getNewUpvotes().get(postID).add(username);
        social.getPosts().get(postID).addVote(username);
    }

    @Override
    public synchronized void addDownvoteTo(int postID, String username) 
            throws ResourceNotFoundException, ForbiddenActionException {
        checkPostUserInteractionValidity(postID, username);
        if(social.getPosts().get(postID).getAuthor().equals(username))
            throw new ForbiddenActionException();
        if(social.getNewDownvotes().putIfAbsent(postID, ConcurrentHashMap.newKeySet())!=null)
            throw new ForbiddenActionException("Downvote already inserted");
        social.getNewDownvotes().get(postID).add(username);
        social.getPosts().get(postID).addDownVote(username);
    }

    // /user
    @Override
    public synchronized void addFollowerTo(String username, String userToFollow) 
            throws ResourceNotFoundException {
        if( social.getUsers().get(username)==null || social.getUsers().get(userToFollow)==null )
            throw new ResourceNotFoundException("User does not exist.");
        social.getUsers().get(userToFollow).addFollower(username);
        social.getUsers().get(username).addFollowing(userToFollow);
    }

    @Override
    public void removeFollowerTo(String username, String userToUnfollow) 
            throws ResourceNotFoundException {
        if( social.getUsers().get(username)==null || social.getUsers().get(userToUnfollow)==null )
            throw new ResourceNotFoundException("User does not exist.");
        social.getUsers().get(userToUnfollow).removeFollower(username);
        social.getUsers().get(username).removeFollowing(userToUnfollow);
    }
    //#endregion

    //#region private DB functions
    /**
     * Check if the Post p is in user 's feed
     *
     * @param user
     * @param p
     * @return
     */
    private boolean checkFeed(User user, Post p) {
        if (!user.getFollowing().contains(p.getAuthor()) && 
                Collections.disjoint(user.getFollowing(), p.getRewinnedBy()) == true)
            return false;
        return true;
    }

    /**
     * Check if the post and the user exist in social and check if user can interact with post
     * @param postID
     * @param username
     * @throws ResourceNotFoundException
     * @throws ForbiddenActionException
     */
    private void checkPostUserInteractionValidity(int postID, String username) 
            throws ResourceNotFoundException, ForbiddenActionException{
        Post post = social.getPosts().get(postID);
        if( post==null )
            throw new ResourceNotFoundException("Post not found.");
        User user = social.getUsers().get(username);
        if( user==null )
            throw new ResourceNotFoundException("User not found.");
        if( !checkFeed(user, post) && !post.getAuthor().equals(username)){
            // user can do an action on a post only if the post is in its feed or it's his own post
            // reject here
            throw new ForbiddenActionException();
        }
    }

    //#endregion

    //#region reward calculation
    @Override
    public void removeIdFromNewUpvotes(Integer id) {
        social.getNewUpvotes().remove(id);
    }

    @Override
    public void removeIdFromNewDownvotes(Integer id) {
        social.getNewDownvotes().remove(id);
    }

    @Override
    public void removeIdFromNewComments(Integer id) {
        social.getNewComments().remove(id);
    }

    @Override
    public synchronized void updateUserWallet(String username, double amountToAdd) 
            throws ResourceNotFoundException {
        User user = social.getUsers().get(username);
        if(user==null)
            throw new ResourceNotFoundException("User not found.");
        user.updateWallet(amountToAdd);
    }

    //#endregion
}
