package messenger;

import java.net.Socket;

/**
 *
 * @author Mathew
 */
public class User {
    Socket socket;
    Thread thread;
    String nickname;
    long uid = -1;
    
    boolean isHandshakeOk;
}
