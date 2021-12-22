package exceptions;

public class TooManyTagsException extends Exception{
    public TooManyTagsException(String message){
        super(message);
    }

    public TooManyTagsException(){
        super("Too many tags. Please choose 5 tags at most.");
    }
}
