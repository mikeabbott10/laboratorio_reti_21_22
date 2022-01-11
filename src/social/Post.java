package social;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;

/**
 * Invariant: ( rewinnedPost!=null && isEmpty(upvotes) && isEmpty(downvotes) ) == true
 */
public @Data class Post implements Comparable<Post> {
    private final long id;
    private String title;
    private String content;
    private String author;
    private final Date date;
    private Set<String> upvotes;
    private Set<String> downvotes;
    private Map<String, HashSet<String>> comments;
    private Set<String> rewinnedBy;

    public Post(long id, String title, String content, String author) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.comments = new HashMap<>();
        this.rewinnedBy = ConcurrentHashMap.newKeySet();
        this.date = Calendar.getInstance().getTime();
        this.upvotes = ConcurrentHashMap.newKeySet();
        this.downvotes = ConcurrentHashMap.newKeySet();
        
    }

    public int compareTo(Post p) {
        return this.id - p.getId() > 0 ? 1 : -1;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Post))
            return false;
        if (((Post) o).getId() == this.id)
            return true;
        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.id);
    }

}
