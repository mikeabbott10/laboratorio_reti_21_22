package exceptions;

public class InvalidUsername extends Exception{
    public InvalidUsername(String message){
        super(message);
    }

    public InvalidUsername(){
        super("Invalid username");
    }
}
