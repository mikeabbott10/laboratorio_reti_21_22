package client.social;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public @Data class Post{
    private int id;
    private String title;
    private String content;
    private String author;
    private Date date;
    private Set<String> upvotes;
    private Set<String> downvotes;
    private ConcurrentSkipListSet<PostComment> comments; // concurrent sorted set
    private Set<String> rewinnedBy;
}
