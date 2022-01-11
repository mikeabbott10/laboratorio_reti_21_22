package social;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class SocialService {
    private ConcurrentHashMap<String, User> users; // users in the social network

    private int idPostCounter; // i will write this in a json file
    private HashSet<String> activeUsernames;
    private HashMap<String, HashSet<User>> tagsUsers;
    private HashMap<Integer, Post> posts;

    // these are consumed by the reward algorithm
    private HashMap<Integer, HashSet<String>> newUpvotes;
    private HashMap<Integer, HashSet<String>> newDownvotes;
    private HashMap<Integer, ArrayList<String>> newComments;

    // Getters n Setters -----------------------------------------------------------------------------
    //#region GetNSet
    public ConcurrentHashMap<String, User> getUsers() {
        return users;
    }
    public void setUsers(ConcurrentHashMap<String, User> users) {
        this.users = users;
    }

    public int getIdPostCounter() {
        return idPostCounter;
    }
    public void setIdPostCounter(int idPostCounter) {
        this.idPostCounter = idPostCounter;
    }

    public HashSet<String> getActiveUsernames() {
        return activeUsernames;
    }
    public void setActiveUsernames(HashSet<String> activeUsernames) {
        this.activeUsernames = activeUsernames;
    }

    public HashMap<String, HashSet<User>> getTagsUsers() {
        return tagsUsers;
    }
    public void setTagsUsers(HashMap<String, HashSet<User>> tagsUsers) {
        this.tagsUsers = tagsUsers;
    }

    public HashMap<Integer, Post> getPosts() {
        return posts;
    }
    public void setPosts(HashMap<Integer, Post> posts) {
        this.posts = posts;
    }

    public HashMap<Integer, HashSet<String>> getNewUpvotes() {
        return newUpvotes;
    }
    public void setNewUpvotes(HashMap<Integer, HashSet<String>> newUpvotes) {
        this.newUpvotes = newUpvotes;
    }

    public HashMap<Integer, HashSet<String>> getNewDownvotes() {
        return newDownvotes;
    }
    public void setNewDownvotes(HashMap<Integer, HashSet<String>> newDownvotes) {
        this.newDownvotes = newDownvotes;
    }

    public HashMap<Integer, ArrayList<String>> getNewComments() {
        return newComments;
    }
    public void setNewComments(HashMap<Integer, ArrayList<String>> newComments) {
        this.newComments = newComments;
    }
    //#endregion

    // Constructors ----------------------------------------------------------------------------------
    public SocialService(ConcurrentHashMap<String, User> users, Set<String> tags, int idPostCounter, 
            HashSet<String> activeUsernames, 
            HashMap<String, HashSet<User>> tagsUsers, 
            HashMap<Integer, Post> posts,
            HashMap<Integer, HashSet<String>> newUpvotes, 
            HashMap<Integer, HashSet<String>> newDownvotes,
            HashMap<Integer, ArrayList<String>> newComments) {
        this.users = users;
        this.idPostCounter = idPostCounter;
        this.activeUsernames = activeUsernames;
        this.tagsUsers = tagsUsers;
        this.posts = posts;
        this.newUpvotes = newUpvotes;
        this.newDownvotes = newDownvotes;
        this.newComments = newComments;
    }

    /**
     * New social network
     */
    public SocialService() {
        this.users = new ConcurrentHashMap<>();
        this.idPostCounter = 0;
        this.activeUsernames = new HashSet<>();
        this.tagsUsers = new HashMap<>();
        this.posts = new HashMap<>();
        this.newUpvotes = new HashMap<>();
        this.newDownvotes = new HashMap<>();
        this.newComments = new HashMap<>();
    }

    
}
