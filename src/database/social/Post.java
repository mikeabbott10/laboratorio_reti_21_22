package database.social;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.fasterxml.jackson.annotation.JsonFilter;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonFilter("postFilter") // ignore some field serializing with this filter name
public @Data class Post implements Comparable<Post> {
    private long id;
    private String title;
    private String content;
    private String author;
    private Date date;
    private Set<String> upvotes;
    private Set<String> downvotes;
    private ConcurrentSkipListSet<PostComment> comments; // concurrent sorted set
    private Set<String> rewinnedBy;
    private int postAge; // the current rewardAlgorithm

    public Post(long id, String title, String content, String author, int rewardCalculatorAge) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.comments = new ConcurrentSkipListSet<>();
        this.rewinnedBy = ConcurrentHashMap.newKeySet();
        this.date = Calendar.getInstance().getTime();
        this.upvotes = ConcurrentHashMap.newKeySet();
        this.downvotes = ConcurrentHashMap.newKeySet();
        this.postAge = rewardCalculatorAge;
        
    }

    public void addComment(String author, String comment){
        PostComment pc = new PostComment(author, comment);
        comments.add(pc);
    }

    public void addVote(String username) {
        upvotes.add(username);
        downvotes.remove(username);
    }

    public void addDownVote(String username) {
        upvotes.remove(username);
        downvotes.add(username);
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
