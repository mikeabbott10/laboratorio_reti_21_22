package social;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashSet;
import java.util.Set;

public class User implements java.io.Serializable, Comparable<User>{
    private String username; // username is the User ID
    private Password password;
    private Set<Long> posts;
    private final String[] tags; // unmodifiable
    private HashSet<String> followers; // users who follow this
    private HashSet<String> following; // users this follows
    private double wallet;


    // Getters n setters ---------------------------------------------------------------------------------
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public Password getPassword() {
        return password;
    }
    public void setPassword(Password password) {
        this.password = password;
    }

    public String[] getTags() {
        return tags;
    }

    public Set<Long> getPosts() {
        return posts;
    }
    public void setPosts(Set<Long> posts) {
        this.posts = posts;
    }
    
    public HashSet<String> getFollowers() {
        return followers;
    }
    public void setFollowers(HashSet<String> followers) {
        this.followers = followers;
    }

    public HashSet<String> getFollowing() {
        return following;
    }
    public void setFollowing(HashSet<String> following) {
        this.following = following;
    }

    public double getWallet() {
        return wallet;
    }
    public void setWallet(double wallet) {
        this.wallet = wallet;
    }

    // Constructors ---------------------------------------------------------------------------------------
    public User(SocialService social, String username, String password, String[] tags) 
                        throws NoSuchAlgorithmException, InvalidKeySpecException{
        this.username = username.trim();
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
        this.setWallet(0);

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
