package database.social;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonFilter;

import database.social.password.Password;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.util.Constants;

@NoArgsConstructor
@JsonFilter("userFilter") // ignore some field serializing with this filter name
public @Data class User implements java.io.Serializable, Comparable<User>{
    private String username; // username is the User ID
    private Password password;
    private Set<Integer> posts;
    private String[] tags; // unmodifiable
    private HashSet<String> followers; // users who follow this
    private HashSet<String> following; // users this follows
    private double wallet;
    private Set<WalletTransaction> wallet_history;
    
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
        this.posts = new HashSet<>();
        this.followers = new HashSet<>();
        this.following = new HashSet<>();
        this.wallet = 0;
        this.wallet_history = ConcurrentHashMap.newKeySet();
    }

    public boolean addPost(int postId) {
        return this.posts.add(postId);
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
        this.wallet+=amountToAdd;
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

}
