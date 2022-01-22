package database.social;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonFilter;

import database.Database;
import database.social.password.Password;
import lombok.NoArgsConstructor;
import server.util.Constants;

@NoArgsConstructor
@JsonFilter("userFilter") // ignore some field serializing with this filter name
public class User implements Comparable<User>{
    private String username; // username is the User ID
    private Password password;
    private Set<Integer>  posts;
    private String[] tags; // unmodifiable
    private Set<String> followers; // users who follow this
    private Set<String>  following; // users this follows
    private AtomicDouble wallet;
    private Set<WalletTransaction> wallet_history;
    private Date last_session = new Date();
    
    public User(SocialService social, String username, String password, String[] tags) 
                        throws NoSuchAlgorithmException, InvalidKeySpecException{
        this.username = username;
        this.password = new Password(password);
        // tags
        this.tags = tags;
        for (String tag : this.tags) {
            social.getTagsUsers().putIfAbsent(tag, new HashSet<String>());
            social.getTagsUsers().get(tag).add(this.username);
        }
        this.posts = ConcurrentHashMap.newKeySet();
        this.followers = ConcurrentHashMap.newKeySet();
        this.following = ConcurrentHashMap.newKeySet();
        this.wallet = new AtomicDouble(0);
        this.wallet_history = ConcurrentHashMap.newKeySet();
        this.last_session = Calendar.getInstance().getTime();
    }

    public User(User u1, Database db){
        this.username = u1.getUsername();
        this.password = u1.getPassword();
        this.tags = u1.getTags();
        this.posts = ConcurrentHashMap.newKeySet();
        this.posts.addAll(u1.getPosts());
        
        synchronized(db.getFollowObj()){
            this.followers = ConcurrentHashMap.newKeySet();
            this.followers.addAll(u1.getFollowers());

            this.following = ConcurrentHashMap.newKeySet();
            this.following.addAll(u1.getFollowing());
        }

        this.wallet = u1.getWallet();
        this.wallet_history = u1.getWallet_history();

        synchronized(u1.getLast_session()){
            this.last_session = u1.getLast_session();
        }
    }

    public boolean addPost(int postId) {
        return this.posts.add(postId);
    }

    public boolean removePost(int postId){
        return this.posts.remove(postId);
    }

    public void addFollower(String username){
        this.followers.add(username);
    }

    public void addFollowing(String username){
        this.following.add(username);
    }

    public void removeFollower(String username){
        this.followers.remove(username);
    }

    public void removeFollowing(String username){
        this.following.remove(username);
    }

    public void updateWallet(double amountToAdd) {
        this.wallet.addDelta(amountToAdd);
        // save transaction
        this.wallet_history.add(new WalletTransaction(
            Constants.DATE_FORMAT.format(Timestamp.valueOf(LocalDateTime.now())), 
            amountToAdd)
        );
    }

    @Override 
    public boolean equals(Object u2){
        if(u2 == null)
            throw new NullPointerException();
        if(!(u2 instanceof User))
            throw new RuntimeException();
        return this.username == ((User)u2).username;
    }
    @Override
    public int compareTo(User u) {
        return this.username.compareTo(u.getUsername());
    }
    @Override
    public int hashCode() {
        return this.username.hashCode();
    }


    public String getUsername() {
        return username;
    }

    public Password getPassword() {
        return password;
    }
    public Set<Integer> getPosts() {
        // sync from outside
        return posts;
    }

    public String[] getTags() {
        return tags;
    }

    public Set<String> getFollowers() {
        // sync from outside
        return followers;
    }

    public Set<String> getFollowing() {
        // sync from outside
        return following;
    }

    public AtomicDouble getWallet() {
        return wallet;
    }

    public Set<WalletTransaction> getWallet_history() {
        return wallet_history;
    }

    public Date getLast_session() {
        return last_session;
    }
    public void setLast_session(Date new_last_session) {
        last_session = new_last_session;
    }

}
