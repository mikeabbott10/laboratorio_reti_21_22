package social;

import java.util.Date;
import java.util.Calendar;

import lombok.Data;


public @Data class PostComment {
    private String author;
    private String comment;
    private Date date;

    public PostComment(String author, String comment){
        this.author = author;
        this.comment = comment;
        this.date = Calendar.getInstance().getTime();
    }
}