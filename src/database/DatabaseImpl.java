package database;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Vector;

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
     *      DELETE FROM posts P WHERE P.id=postID;
     * @param user the author of the post
     * @param postID the post id
     * @return true if the post is removed
     */
    @Override
    public boolean removePost(User user, int postID){
        if( user.getPosts().remove(postID) == false )
            return false; // postID is not a post from user
        Post p = social.getPosts().remove(postID);
        if( p != null ){
            p.getRewinnedBy().forEach((String username) -> {
                social.getUsers().get(username).getPosts().remove(postID);
            });
            return true;
        }
        assert true == false; // never here
        return false; // postID is not a post from user
    }

    /**
     * query simulation:
     *      INSERT INTO posts (Title, Content, Author) VALUES (title, content, author);
     * @param title
     * @param content
     * @param author the username of the author
     * @throws DatabaseException
     */
    @Override
    public int createPost(String title, String content, String author) throws DatabaseException{
        int postId = social.postIdCounter++;
        Post post = new Post(postId, title, content, author);
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
        return (Post[]) posts.toArray();
    }

    /**
     * query simulation:
     *      SELECT * FROM users U WHERE <U.tag contains string>
     */
    @Override
    public HashMap<String, User> getUsersFromTagname(String tagname) {
        HashMap<String, User> users = new HashMap<>();
        social.getUsers().forEach((username, user) -> {
            for (String tag : user.getTags()) {
                if(tag.equals(tagname) ){
                    users.put(username, user);
                    break;
                }
            }
        });

        return users;
    }

    /**
     * query simulation:
     *      INSERT INTO comments (PostID, Comment, Author) VALUES (postID, content, author);
     */
    @Override
    public void addComment(int postID, String comment, String author){
        social.getPosts().get(postID).addComment(author, comment);
    }

    @Override
    public void rewinPost(int postId, String author) {
        
    }

    @Override
    public void addFollowerTo(String username, String userToFollow) {
        social.getUsers().get(userToFollow).addFollower(username);
        social.getUsers().get(username).addFollowing(userToFollow);
    }

    @Override
    public void removeFollowerTo(String username, String userToUnfollow) {
        social.getUsers().get(userToUnfollow).removeFollower(username);
        social.getUsers().get(username).removeFollowing(userToUnfollow);
    }

    @Override
    public void addVoteTo(int postID, String username) {
        social.getPosts().get(postID).addVote(username);
        
    }

    @Override
    public void addDownvoteTo(int postID, String username) {
        social.getPosts().get(postID).addDownVote(username);
        
    }


    //#endregion


    //#region middleware
    
    //#endregion
    
}
