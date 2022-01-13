package exceptions;

public class ForbiddenActionException extends Exception{
    public ForbiddenActionException(String message){
        super(message);
    }

    public ForbiddenActionException(){
        super("Action forbidden.");
    }
}
