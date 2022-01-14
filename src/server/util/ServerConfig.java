package server.util;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public @Data class ServerConfig implements java.io.Serializable{
    // rmi
    public int SERVER_RMI_PORT = 25258;
    public String SERVER_IP = "localhost";
    public String RMIServerUrl = "rmi://"+ SERVER_IP +":" + SERVER_RMI_PORT;
    public String rmiServiceName = "/winsomeservice";

    //tcp
    public int HTTP_SERVER_PORT = 8080;

    //multicast
    public int MULTICAST_PORT = 40000;
    // IPV4: indirizzo di un gruppo è un indirizzo in classe D [224.0.0.0 – 239.255.255.255]
    public String MULTICAST_ADDRESS = "239.255.1.3"; // [239.0.0.0 - 239.255.255.255] : Local multicast addresses

    public double AUTHOR_PERCENTAGE = 0.7;
    public String BACKUP_DIRECTORY = "./server/bkp/";
    public long REWARD_TIMEOUT = 10000L;
    public long BACKUP_TIMEOUT = 10000L;

}
