package exceptions;

public class InvalidTags extends Exception{
    public InvalidTags(String message){
        super(message);
    }

    public InvalidTags(){
        super("Invalid tags");
    }
}
