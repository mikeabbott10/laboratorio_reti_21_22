package database;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Collections;
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

    // atomicity wrappers
    public final Object commentObj = new Object();
    public final Object rewinObj = new Object();
    public final Object rateObj = new Object();
    public final Object followObj = new Object();
    
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
    public void removePost(String username, int postID) 
            throws ResourceNotFoundException{
        User us = social.getUsers().get(username);
        if(us==null)
            throw new ResourceNotFoundException("User does not exists.");
        if( us.removePost(postID) == false )
            throw new ResourceNotFoundException("Post not found");
        Post p = social.removePost(postID);
        if( p != null ){
            // p.getRewinnedBy can be modified by this.rewinPost
            p.getRewinnedBy().forEach((String uname) -> {
                User u = social.getUsers().get(uname);
                if(u!=null){
                    u.removePost(postID);
                }
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
    public int createPost(String title, String content, String author) 
            throws DatabaseException, ForbiddenActionException{
        if(content.length()>500 || title.length()>20){
            throw new ForbiddenActionException("Invalid content or title.");
        }
        int postId = social.getPostIdCounter().getAndIncrement(); // atomically increments by one
        Post post = new Post(postId, title, content, author, social.getRewardRoutineAge().get());
        User user = social.getUsers().get(author);
        // *** what if i create the author here? (it's possible, it's multithreading)
        // i don't care. Too many chances this won't happen, just a failed post creation.
        if(user == null){
            throw new ForbiddenActionException("Not allowed. Please retry.");
        }
        if( user.addPost(postId) == false || social.addPost(postId, post) != null) 
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
        User  u;
        try{
            u = new User( social.getUsers().get(username), this );
        }catch(NullPointerException e){
            return null;
        }
        try{
            for (int postID : u.getPosts()) {
                Post p = new Post( social.getPosts().get(postID) );
                if(p!=null)
                    posts.add( p );
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


    // rewardThread calls the next 4 methods
    /**
     * add 1 to reward routine age.
     */
    @Override
    public int updateRewardIterations() {
        return social.getRewardRoutineAge().incrementAndGet();
        
    }
    @Override
    public ConcurrentHashMap<Integer, KeySetView<String, Boolean>> getNewUpvotes() {
        synchronized(this.rateObj){
            return social.getNewUpvotes();
        }
    }
    @Override
    public ConcurrentHashMap<Integer, KeySetView<String, Boolean>> getNewDownvotes() {
        synchronized(this.rateObj){
            return social.getNewDownvotes();
        }
    }
    @Override
    public ConcurrentHashMap<Integer, KeySetView<String, Boolean>> getNewComments() {
        synchronized(this.commentObj){
            return social.getNewComments();
        }
    }

    // called by BackupThread and CleanRoutine
    @Override
    public SocialService getSocialInstance() {
        return social;
    }

    //#endregion  


    @Override
    public ConcurrentHashMap<Integer, Post> getPosts() {
        return social.getPosts();
    }

    // /post
    @Override
    public void addComment(int postID, String comment, String author) 
            throws ResourceNotFoundException, ForbiddenActionException{
        Object[] o = checkPostUserInteractionValidity(postID, author);
        Post post = (Post) o[0];
        //User user = (User) o[1];
        if(post.addComment(author, comment) == false)
            throw new ForbiddenActionException("Cannot add this comment.");
        // if at *** i use call getNewComments from rewardThread i get inconsistency: 
        // need atomicity here
        synchronized(this.commentObj){
            social.getNewComments().putIfAbsent(postID, ConcurrentHashMap.newKeySet());
            // ***
            social.getNewComments().get(postID).add(author);
        }
    }

    @Override
    public void rewinPost(int postID, String rewinner) 
            throws ResourceNotFoundException, ForbiddenActionException {
        Object[] o = checkPostUserInteractionValidity(postID, rewinner);
        Post post = (Post) o[0];
        User user = (User) o[1];
        // need atomicity here
        synchronized(this.rewinObj){
            user.addPost(postID);
            // ***
            post.addRewinnedBy(rewinner);
        }
    }
    
    @Override
    public void addVoteTo(int postID, String username) 
            throws ResourceNotFoundException, ForbiddenActionException {
        Object[] o = checkPostUserInteractionValidity(postID, username);
        Post post = (Post) o[0];
        //User user = (User) o[1];
        if(post.getAuthor().equals(username))
            throw new ForbiddenActionException();
        // need atomicity here
        synchronized(this.rateObj){
            if(social.getNewUpvotes().putIfAbsent(postID, ConcurrentHashMap.newKeySet())!=null)
                throw new ForbiddenActionException("Upvote already inserted");
            // ***
            social.getNewUpvotes().get(postID).add(username);
            // ***
            post.addVote(username);
        }
    }

    @Override
    public void addDownvoteTo(int postID, String username) 
            throws ResourceNotFoundException, ForbiddenActionException {
        Object[] o = checkPostUserInteractionValidity(postID, username);
        Post post = (Post) o[0];
        //User user = (User) o[1];
        if(post.getAuthor().equals(username))
            throw new ForbiddenActionException();
        // need atomicity here
        synchronized(this.rateObj){
            if(social.getNewDownvotes().putIfAbsent(postID, ConcurrentHashMap.newKeySet())!=null)
                throw new ForbiddenActionException("Downvote already inserted");
            // ***
            social.getNewDownvotes().get(postID).add(username);
            // ***
            post.addDownVote(username);
        }
    }

    // /user
    @Override
    public void addFollowerTo(String username, String userToFollow) 
            throws ResourceNotFoundException, ForbiddenActionException {
        User user = social.getUsers().get(username);
        if( user==null || social.getUsers().get(userToFollow)==null )
            throw new ResourceNotFoundException("User does not exist.");
        if(username.equals(userToFollow))
            throw new ForbiddenActionException("Cannot follow yourself.");
        if(user.getFollowing().contains(userToFollow))
            throw new ForbiddenActionException("User already followed.");
        // need atomicity here
        synchronized(this.followObj){
            social.getUsers().get(userToFollow).addFollower(username);
            // *** 
            social.getUsers().get(username).addFollowing(userToFollow);
        }
    }

    @Override
    public void removeFollowerTo(String username, String userToUnfollow) 
            throws ResourceNotFoundException, ForbiddenActionException {
        User user = social.getUsers().get(username);
        if( user==null || social.getUsers().get(userToUnfollow)==null )
            throw new ResourceNotFoundException("User does not exist.");
        if(username.equals(userToUnfollow))
            throw new ForbiddenActionException("Cannot follow yourself.");
        if(!user.getFollowing().contains(userToUnfollow))
            throw new ForbiddenActionException("User not followed yet.");
        // need atomicity here
        synchronized(this.followObj){
            social.getUsers().get(userToUnfollow).removeFollower(username);
            // *** 
            social.getUsers().get(username).removeFollowing(userToUnfollow);
        }
    }

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
     * @return non null {Post instance, User instance}
     * @throws ResourceNotFoundException
     * @throws ForbiddenActionException
     */
    private Object[] checkPostUserInteractionValidity(int postID, String username) 
            throws ResourceNotFoundException, ForbiddenActionException{
        Post post = social.getPosts().get(postID);
        if( post==null )
            throw new ResourceNotFoundException("Post not found.");
        User user = social.getUsers().get(username);
        if( user==null )
            throw new ResourceNotFoundException("User not found.");
        if( !checkFeed(new User(user,this), new Post(post)) && !post.getAuthor().equals(username)){
            // user can do an action on a post only if the post is in its feed or it's his own post
            // reject here
            throw new ForbiddenActionException();
        }
        return new Object[]{post, user};
    }

    //#endregion

    //#region reward calculation
    @Override
    public void removeIdFromNewUpvotes(Integer id) {
        synchronized(this.rateObj){
            social.getNewUpvotes().remove(id);
        }
    }

    @Override
    public void removeIdFromNewDownvotes(Integer id) {
        synchronized(this.rateObj){
            social.getNewDownvotes().remove(id);
        }
    }

    @Override
    public void removeIdFromNewComments(Integer id) {
        synchronized(this.commentObj){
            social.getNewComments().remove(id);
        }
    }

    @Override
    public void updateUserWallet(String username, double amountToAdd) 
            throws ResourceNotFoundException {
        User user = social.getUsers().get(username);
        if(user==null)
            throw new ResourceNotFoundException("User not found.");
        user.updateWallet(amountToAdd);
    }

    //#endregion


    public Object getCommentObj() {
        return commentObj;
    }

    public Object getRewinObj() {
        return rewinObj;
    }

    public Object getRateObj() {
        return rateObj;
    }

    public Object getFollowObj() {
        return followObj;
    }
}
