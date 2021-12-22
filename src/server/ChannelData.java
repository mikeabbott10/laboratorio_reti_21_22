package server;

/**
 * Useful data related to SocketChannel instances.
 * i.e. useful when we read just a part of the payload and we need more reads to get it all.
 * 
 * message.length == previously_read_bytes
 */
public class ChannelData {
    private long payload_length;
    private long previously_read_bytes;
    private String message;

    public ChannelData() {
        this.payload_length = 0;
        this.previously_read_bytes = 0;
        this.message = "";
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getPayload_length() {
        return payload_length;
    }

    public long getPreviously_read_bytes() {
        return previously_read_bytes;
    }

    public void setPreviously_read_bytes(long previously_read_bytes) {
        this.previously_read_bytes = previously_read_bytes;
    }

    public void setPayload_length(long payload_length) {
        this.payload_length = payload_length;
    }

}
