package social;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonFilter;

import lombok.Data;
import lombok.NoArgsConstructor;
import social.password.Password;

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
    private Map<Date, Integer> walletHistory;
    
    public User(SocialService social, String username, String password, String[] tags) 
                        throws NoSuchAlgorithmException, InvalidKeySpecException{
        this.username = username;
        this.password = new Password(password);
        // tags
        this.tags = tags;
        for (String tag : this.tags) {
            social.getTagsUsers().putIfAbsent(tag, new HashSet<User>());
            social.getTagsUsers().get(tag).add(this);
        }
        this.posts = new HashSet<>();
        this.followers = new HashSet<>();
        this.following = new HashSet<>();
        this.wallet = 0;
        this.walletHistory = new ConcurrentHashMap<>();
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
