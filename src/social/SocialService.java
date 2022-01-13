package social;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;

public @Data class SocialService implements java.io.Serializable{
    private ConcurrentHashMap<String, User> users; // users in the social network
    private ConcurrentHashMap<Integer, Post> posts; // post in the social network
    public int postIdCounter; // id counter of posts in social network
    private ConcurrentHashMap<String, HashSet<String>> tagsUsers; // tag name to usernames

    private Set<String> loggedUsers;

    // for reward calculation
    private ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<String, Boolean>> newUpvotes;
    private ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<String, Boolean>> newDownvotes;
    private ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<String, Boolean>> newComments;

    // Constructors ----------------------------------------------------------------------------------
    public SocialService(ConcurrentHashMap<String, User> users, Set<String> tags, int postIdCounter, 
            ConcurrentHashMap<String, HashSet<String>> tagsUsers, 
            ConcurrentHashMap<Integer, Post> posts,
            ConcurrentHashMap.KeySetView<String, Boolean> loggedUsers,
            ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<String, Boolean>> newUpvotes, 
            ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<String, Boolean>> newDownvotes,
            ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<String, Boolean>> newComments) {
        this.users = users;
        this.postIdCounter = postIdCounter;
        this.tagsUsers = tagsUsers;
        this.posts = posts;
        this.loggedUsers = loggedUsers;
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
        this.tagsUsers = new ConcurrentHashMap<>();
        this.posts = new ConcurrentHashMap<>();
        this.loggedUsers = ConcurrentHashMap.newKeySet();
        this.newUpvotes = new ConcurrentHashMap<>();
        this.newDownvotes = new ConcurrentHashMap<>();
        this.newComments = new ConcurrentHashMap<>();
    }

}
