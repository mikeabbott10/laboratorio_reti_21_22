package database.social;

import java.util.Date;
import java.util.Calendar;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public @Data class PostComment implements Comparable<PostComment>{
    private String author;
    private String comment;
    private Date date;

    public PostComment(String author, String comment){
        this.author = author;
        this.comment = comment;
        this.date = Calendar.getInstance().getTime();
    }

    @Override
    public int compareTo(PostComment o) {
        return this.date.compareTo(o.getDate());
    }
}
