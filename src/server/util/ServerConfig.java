package server.util;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public @Data class ServerConfig{
    // rmi
    public int SERVER_RMI_PORT;
    public String SERVER_IP;
    public String RMIServerUrl;
    public String rmiServiceName;

    //tcp
    public int HTTP_SERVER_PORT;

    //multicast
    public int MULTICAST_PORT;
    // IPV4: indirizzo di un gruppo è un indirizzo in classe D [224.0.0.0 – 239.255.255.255]
    public String MULTICAST_ADDRESS; // [239.0.0.0 - 239.255.255.255] : Local multicast addresses

    public double AUTHOR_PERCENTAGE;
    public String BACKUP_DIRECTORY;
    public long REWARD_TIMEOUT;
    public long BACKUP_TIMEOUT;

}
