package server.nio;

/**
 * Useful data related to SocketChannel instances.
 * i.e. useful when we read just a part of the request and we need more reads to get it all.
 * 
 * message.length == previously_read_bytes
 */
public class ChannelData {
    private long missingBytes;
    private String message;

    public ChannelData() {
        this.setMissingBytes(0);
        this.message = "";
    }

    public long getMissingBytes() {
        return missingBytes;
    }

    public void setMissingBytes(long missingBytes) {
        this.missingBytes = missingBytes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}