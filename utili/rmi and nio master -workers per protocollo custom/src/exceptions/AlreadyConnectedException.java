package exceptions;

public class AlreadyConnectedException extends Exception{
    public AlreadyConnectedException(String message){
        super(message);
    }

    public AlreadyConnectedException(){
        super("Already connected host");
    }
}
