package database.social;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonFilter;

import database.Database;
import lombok.Data;

@JsonFilter("socialFilter") // ignore some field serializing with this filter name
public @Data class SocialService{
    private ConcurrentHashMap<String, User> users; // users in the social network

    // synchronized structure bc threads can remove posts
    private ConcurrentHashMap<Integer, Post> posts; // post in the social network

    // AtomicInteger class stores its value field in a volatile variable
    // (more threads can use cached val. volatile avoid this.)
    private AtomicInteger postIdCounter; // id counter of posts in social network
    private AtomicInteger rewardRoutineAge; // reward algorithm age
    
    private ConcurrentHashMap<String, HashSet<String>> tagsUsers; // tag name to usernames

    private Set<String> loggedUsers;

    // for reward calculation
    private ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<String, Boolean>> newUpvotes;
    private ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<String, Boolean>> newDownvotes;
    private ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<String, Boolean>> newComments;

    // Constructors ----------------------------------------------------------------------------------
    public SocialService(ConcurrentHashMap<String, User> users, Set<String> tags, 
            AtomicInteger postIdCounter, AtomicInteger rewardRoutineAge, 
            ConcurrentHashMap<String, HashSet<String>> tagsUsers, 
            ConcurrentHashMap<Integer, Post> posts,
            ConcurrentHashMap.KeySetView<String, Boolean> loggedUsers,
            ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<String, Boolean>> newUpvotes, 
            ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<String, Boolean>> newDownvotes,
            ConcurrentHashMap<Integer, ConcurrentHashMap.KeySetView<String, Boolean>> newComments) {
        this.rewardRoutineAge = rewardRoutineAge;
        this.users = users;
        this.postIdCounter = postIdCounter;
        this.tagsUsers = tagsUsers;
        this.posts = posts;
        this.loggedUsers = loggedUsers;
        this.newUpvotes = newUpvotes;
        this.newDownvotes = newDownvotes;
        this.newComments = newComments;
    }

    public SocialService(SocialService s, Database db) {
        this.rewardRoutineAge = s.getRewardRoutineAge();
        this.users = s.getUsers();
        this.postIdCounter = s.getPostIdCounter();
        this.tagsUsers = s.getTagsUsers();
        this.posts = s.getPosts();
        this.loggedUsers = s.getLoggedUsers();
        synchronized(db.getRateObj()){
            this.newUpvotes = s.getNewUpvotes();
            this.newDownvotes = s.getNewDownvotes();
        }
        synchronized(db.getCommentObj()){
            this.newComments = s.getNewComments();
        }
    }

    /**
     * New social network
     */
    public SocialService() {
        this.rewardRoutineAge = new AtomicInteger(0);
        this.users = new ConcurrentHashMap<>();
        this.postIdCounter = new AtomicInteger(0);
        this.tagsUsers = new ConcurrentHashMap<>();
        this.posts = new ConcurrentHashMap<>();
        this.loggedUsers = ConcurrentHashMap.newKeySet();
        this.newUpvotes = new ConcurrentHashMap<>();
        this.newDownvotes = new ConcurrentHashMap<>();
        this.newComments = new ConcurrentHashMap<>();
    }

    public Object addPost(int postId, Post post) {
        return this.posts.put(postId, post);
    }

    public Post removePost(int postId) {
        return this.posts.remove(postId);
    }

}
