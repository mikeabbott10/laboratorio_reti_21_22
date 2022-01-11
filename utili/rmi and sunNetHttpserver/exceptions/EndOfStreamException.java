package exceptions;

public class EndOfStreamException extends Exception{
    public EndOfStreamException(String message){
        super(message);
    }

    public EndOfStreamException(){
        super("End of stream exception occurred");
    }
}
