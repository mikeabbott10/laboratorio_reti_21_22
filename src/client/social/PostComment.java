package client.social;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public @Data class PostComment implements Comparable<PostComment>{
    private String author;
    private String comment;
    private Date date;

    @Override
    public int compareTo(PostComment o) {
        return this.date.compareTo(o.getDate());
    }
}