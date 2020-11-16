package messenger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author Mathew
 */
public class Client {
    
    static final byte PROTOCOL_VERSION = 3;
    static final byte DISCONNECT_PACKET = -2, UNKNOWN_PACKET = -1, HELLO_PACKET = 0, UID_REQUEST_PACKET = 1, SEND_MESSAGE_PACKET = 2;
    User user;
    DataInputStream in;
    DataOutputStream out;
    
    static void info(String s) {
        System.out.println(s);
    }
    
    public void connect(String ip) throws Exception {
        user = new User();
        user.socket = new Socket(ip, 8989);
        user.socket.setKeepAlive(true);
        
        connected();
    }
    
    public void authorize(String nickname) { 
        if (user == null) throw new NullPointerException("User is null. You should connect first.");
        
        user.thread = new Thread() {
            public void run() {
                try {
                    initConnection(nickname);
                } catch (Exception e) {
                    try {
                        closeConnection(e);
                    } catch (Exception ex) {
                        disconnect(ex);
                    }
                }
            }
        };
        user.thread.start();
    }
    
    public void closeConnection(Exception ex) throws Exception {
        if (user == null) throw new NullPointerException("User is null. You should connect first.");
        
        sendDisconnect(out);
        closedConnection(ex);
        disconnect(ex);
    }
    
    public void closeConnection() throws Exception {
        closeConnection(null);
    }
    
    public void disconnect(Exception ex) {
        //if (user == null) throw new NullPointerException("User is null. You should connect first.");
        
        try {
            if (!user.socket.isClosed()) user.socket.close();
            info("Disconnecting from " + user.socket.getInetAddress().getHostAddress());
        } catch (Exception e) {}
        user = null;
        in = null;
        out = null;
        
        disconnected(ex);
    }
    
    public void disconnect() {
        disconnect(null);
    }

    private void initConnection(String nickname) throws Exception {
        in = new DataInputStream(user.socket.getInputStream());
        out = new DataOutputStream(user.socket.getOutputStream());
        
        sendHello(out);
        processHello(in);
        saidHello();
        
        sendNicknameRequest(nickname, out);
        processNicknameRequest(in);
        authorized();
        
        while (!user.socket.isClosed()) {
            processPacket(user, in, out);
        }
        
        closeConnection(null);
    }

    private void sendHello(DataOutputStream out) throws Exception {
        out.writeByte(HELLO_PACKET);
        out.writeByte(PROTOCOL_VERSION);
    }

    private void processHello(DataInputStream in) throws Exception {
        byte id = in.readByte();
        if (id != HELLO_PACKET) throw new IOException("Not a hello packet: " + id);
        byte version = in.readByte();
        if (version != PROTOCOL_VERSION) throw new IOException("Incompatiable protocol version: " + version);
        user.isHandshakeOk = true;
    }

    private void sendNicknameRequest(String nickname, DataOutputStream out) throws Exception {
        out.writeByte(UID_REQUEST_PACKET);
        out.writeUTF(nickname);
        user.nickname = nickname;
    }
    
    private void processNicknameRequest(DataInputStream in) throws Exception {
        byte id = in.readByte();
        if (id != UID_REQUEST_PACKET) throw new IOException("Not a uid request: " + id);
        long uid = in.readLong();
        if (uid < 0) {
            user.nickname = null;
            throw new IOException("Got UID request error: " + uid);
        }
        user.uid = uid;
    }

    private void sendDisconnect(DataOutputStream out) throws Exception {
        out.writeByte(DISCONNECT_PACKET);
    }

    private void processPacket(User user, DataInputStream in, DataOutputStream out) throws Exception {
        byte id = in.readByte();
        gotNewPacket(id);
        
        switch (id) {
            case SEND_MESSAGE_PACKET:
                processSendMessage(in);
                break;
        }
    }

    private void processSendMessage(DataInputStream in) throws Exception {
        String nickname = in.readUTF();
        String message = in.readUTF();
        
        if (nickname != null && message != null) gotNewMessage(nickname, message);
    }

    public void gotNewMessage(String nickname, String message) {
        info("Got a new message " + message + " from " + nickname);
    }
    
    public void disconnected(Exception e) {
        info("Got disconnected, reason: " + e);
    }
    
    public void connected() {
        info("Got connected to " + user.socket.getInetAddress().getHostAddress());
    }
    
    public void authorized() {
        info("Got user id " + user.uid + " and nickname " + user.nickname);
    }
    
    public void closedConnection(Exception e) {
        info("Closed connection, reason: " + e);
    }
    
    public void saidHello() {
        info("Hello handshake is ok.");
    }
    
    public void gotNewPacket(byte id) {
        info("Got a packet id " + id + " from " + user.socket.getInetAddress().getHostAddress());
    }
    
    public void sendMessage(String message) throws Exception {
        if (user == null || out == null) throw new NullPointerException("User is not connected. You should connect first.");
        if (user.uid < 0) throw new NullPointerException("User is not authorized. You should authorize first.");
        if (message == null || message.trim().isEmpty()) throw new NullPointerException("Message is empty.");
        
        out.writeByte(SEND_MESSAGE_PACKET);
        out.writeUTF(message);
    }
}
