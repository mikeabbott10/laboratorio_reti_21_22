package social;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Invariant: ( rewinnedPost!=null && isEmpty(upvotes) && isEmpty(downvotes) ) == true
 */
public class Post {
    private final long id;
    private String title;
    private String content;
    private User author;
    //private final Post rewinnedPost;
    private final Date date;
    /*private Set<User> upvotes;
    private Set<User> downvotes;*/

    public Post(long id, String title, String content, User author) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        //this.rewinnedPost = null;
        this.date = Calendar.getInstance().getTime();
        /*this.upvotes = ConcurrentHashMap.newKeySet(); // Concurrent set of Post instances;
        this.downvotes = ConcurrentHashMap.newKeySet(); // Concurrent set of Post instances;
        */
    }
    /*public Post(long id, String title, String content, User author, Post rewinnedPost) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.rewinnedPost = rewinnedPost;
        this.date = Calendar.getInstance().getTime();
        this.upvotes = ConcurrentHashMap.newKeySet(); // Concurrent set of Post instances;
        this.downvotes = ConcurrentHashMap.newKeySet(); // Concurrent set of Post instances;
        
    }*/

}
