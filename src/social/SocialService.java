package social;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;


public @Data class SocialService {
    private ConcurrentHashMap<String, User> users; // users in the social network

    public int postIdCounter;
    private HashSet<String> activeUsernames;
    private HashMap<String, HashSet<User>> tagsUsers;
    private HashMap<Integer, Post> posts;

    // these are consumed by the reward algorithm
    private HashMap<Integer, HashSet<String>> newUpvotes;
    private HashMap<Integer, HashSet<String>> newDownvotes;
    private HashMap<Integer, ArrayList<String>> newComments;

    // Constructors ----------------------------------------------------------------------------------
    public SocialService(ConcurrentHashMap<String, User> users, Set<String> tags, int postIdCounter, 
            HashSet<String> activeUsernames, 
            HashMap<String, HashSet<User>> tagsUsers, 
            HashMap<Integer, Post> posts,
            HashMap<Integer, HashSet<String>> newUpvotes, 
            HashMap<Integer, HashSet<String>> newDownvotes,
            HashMap<Integer, ArrayList<String>> newComments) {
        this.users = users;
        this.postIdCounter = postIdCounter;
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
        this.postIdCounter = 0;
        this.activeUsernames = new HashSet<>();
        this.tagsUsers = new HashMap<>();
        this.posts = new HashMap<>();
        this.newUpvotes = new HashMap<>();
        this.newDownvotes = new HashMap<>();
        this.newComments = new HashMap<>();
    }

}
