package client.util;

public class Constants {
    // rmi
    static final int SERVER_RMI_PORT = 25258;
    static final String SERVER_IP = "localhost";
    static final String RMIServerUrl = "rmi://"+ SERVER_IP +":" + SERVER_RMI_PORT;
    static final String rmiServiceName = "/winsomeservice";

    //http
    static final int HTTP_SERVER_PORT = 8080;

    //multicast
    int MULTICAST_PORT = 40000;
    // IPV4: indirizzo di un gruppo è un indirizzo in classe D [224.0.0.0 – 239.255.255.255]
    String MULTICAST_ADDRESS = "239.255.1.3"; // [239.0.0.0 - 239.255.255.255] : Local multicast addresses
}
