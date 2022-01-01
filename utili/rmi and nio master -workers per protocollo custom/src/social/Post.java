package social;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Invariant: ( rewinnedPost!=null && isEmpty(upvotes) && isEmpty(downvotes) ) == true
 */
public class Post {
    private String title;
    private String text;
    private User author;
    private final Post rewinnedPost;
    private final Date date;
    private Set<User> upvotes;
    private Set<User> downvotes;

    public Post(String title, String text, User author) {
        this.title = title;
        this.text = text;
        this.author = author;
        this.rewinnedPost = null;
        this.date = Calendar.getInstance().getTime();
        this.upvotes = ConcurrentHashMap.newKeySet(); // Concurrent set of Post instances;
        this.downvotes = ConcurrentHashMap.newKeySet(); // Concurrent set of Post instances;
    }
    public Post(String title, String text, User author, Post rewinnedPost) {
        this.title = title;
        this.text = text;
        this.author = author;
        this.rewinnedPost = rewinnedPost;
        this.date = Calendar.getInstance().getTime();
        this.upvotes = ConcurrentHashMap.newKeySet(); // Concurrent set of Post instances;
        this.downvotes = ConcurrentHashMap.newKeySet(); // Concurrent set of Post instances;
    }

}
