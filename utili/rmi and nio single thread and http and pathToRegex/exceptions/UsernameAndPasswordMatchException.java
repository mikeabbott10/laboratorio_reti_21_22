package exceptions;

public class UsernameAndPasswordMatchException extends Exception{
    public UsernameAndPasswordMatchException(String message){
        super(message);
    }

    public UsernameAndPasswordMatchException(){
        super("Username and password don't match");
    }
}
