package social;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFilter;

import lombok.Data;
import lombok.NoArgsConstructor;
import social.password.Password;

@NoArgsConstructor
@JsonFilter("userPasswordFilter") // ignoring the password field if serialize with this filter name
public @Data class User implements java.io.Serializable, Comparable<User>{
    private String username; // username is the User ID
    private Password password;
    private Set<Integer> posts;
    private String[] tags; // unmodifiable
    private HashSet<String> followers; // users who follow this
    private HashSet<String> following; // users this follows
    private double wallet;
    
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
        this.wallet = 0;

    }

    public boolean addPost(int postId) {
        return this.posts.add(postId);
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
