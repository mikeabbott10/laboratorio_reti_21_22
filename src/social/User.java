package social;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class User implements java.io.Serializable{
    private String username; // username is the User ID
    private Password password;
    private Set<String> tags;

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

    public Set<String> getTags() {
        return tags;
    }
    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    // Constructor ---------------------------------------------------------------------------------------
    public User(String username, String password, String[] tags) 
                        throws NoSuchAlgorithmException, InvalidKeySpecException{
        this.username = username.trim();
        this.password = new Password(password);
        this.tags = ConcurrentHashMap.newKeySet(); // Concurrent set of String type;
    }


    @Override public boolean equals(Object u2){
        if(u2 == null)
            throw new NullPointerException();
        if(!(u2 instanceof User))
            throw new RuntimeException();
        return this.username == ((User)u2).username;
    }
}
