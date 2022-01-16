package client.util;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public @Data class ClientConfig implements java.io.Serializable{
    // rmi
    public int SERVER_RMI_PORT = 25258;
    public String SERVER_IP = "localhost";
    public String RMIServerUrl = "rmi://"+ SERVER_IP +":" + SERVER_RMI_PORT;
    public String rmiServiceName = "/winsomeservice";

    //tcp
    public int HTTP_SERVER_PORT = 8080;

}
