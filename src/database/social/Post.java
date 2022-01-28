package database.social;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.fasterxml.jackson.annotation.JsonFilter;

import exceptions.ForbiddenActionException;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonFilter("postFilter") // ignore some field serializing with this filter name
public class Post implements java.io.Serializable, Comparable<Post> {
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

    public Post(Post p){
        this.id = p.getId();
        this.title = p.getTitle();
        this.content = p.getContent();
        this.author = p.getAuthor();
        this.comments = new ConcurrentSkipListSet<>();
        this.comments.addAll(p.getComments());
        this.rewinnedBy = ConcurrentHashMap.newKeySet();
        this.rewinnedBy.addAll(p.getRewinnedBy());
        this.date = p.getDate();
        this.upvotes = ConcurrentHashMap.newKeySet();
        this.upvotes.addAll(p.getUpvotes());
        this.downvotes = ConcurrentHashMap.newKeySet();
        this.downvotes.addAll(p.getDownvotes());
        this.postAge = p.getPostAge();
    }

    public boolean addComment(String commentAuthor, String comment){
        if(this.author == commentAuthor){
            return false;
        }
        PostComment pc = new PostComment(commentAuthor, comment);
        return comments.add(pc);
    }

    public void addVote(String username) throws ForbiddenActionException {
        if(!downvotes.contains(username)){
            if(!upvotes.add(username))
                throw new ForbiddenActionException("Vote already inserted");
        }else
            throw new ForbiddenActionException("Vote already inserted");
    }

    public void addDownVote(String username) throws ForbiddenActionException {
        if(!upvotes.contains(username)){
            if(!downvotes.add(username))
                throw new ForbiddenActionException("Vote already inserted");
        }else
            throw new ForbiddenActionException("Vote already inserted");
    }

    public boolean addRewinnedBy(String username) {
        return rewinnedBy.add(username);
    }

    public boolean removeRewinnedBy(String username) {
        return rewinnedBy.remove(username);
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

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getAuthor() {
        return author;
    }

    public Date getDate() {
        return date;
    }

    public Set<String> getUpvotes() {
        return upvotes;
    }

    public Set<String> getDownvotes() {
        return downvotes;
    }

    public ConcurrentSkipListSet<PostComment> getComments() {
        return comments;
    }

    public Set<String> getRewinnedBy() {
        // sync from outside
        return rewinnedBy;
    }

    public int getPostAge() {
        return postAge;
    }    

}
