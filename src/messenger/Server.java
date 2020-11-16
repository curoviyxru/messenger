package messenger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Mathew
 */
public class Server {
    
    static final byte PROTOCOL_VERSION = 3;
    static final byte DISCONNECT_PACKET = -2, UNKNOWN_PACKET = -1, HELLO_PACKET = 0, UID_REQUEST_PACKET = 1, SEND_MESSAGE_PACKET = 2;
    ServerSocket serverSocket;
    List<User> users = new ArrayList<>();
    long nextUID;
    
    static void info(String s) {
        System.out.println(s);
    }
    
    public void start() throws Exception {
        nextUID = 1;
        serverSocket = new ServerSocket(8989);
        
        info("Server has started.");
        
        while (serverSocket != null && !serverSocket.isClosed()) {
            Socket socket = serverSocket.accept();
            if (socket != null) initSocket(socket);
        }
    }
    
    public void stop() throws Exception {
        for (User u : users) {
            deleteSocket(u, null);
        }
        try {
            if (!serverSocket.isClosed()) serverSocket.close();
        } catch (Exception e) {}
        serverSocket = null;
        nextUID = 0;
        users.clear();
        
        info("Server has stopped.");
    }

    private void initSocket(Socket socket) throws Exception {
        info("Got a new socket: " + socket.getInetAddress().getHostAddress());
        socket.setKeepAlive(true);
        User user = new User();
        user.socket = socket;
        
        user.thread = new Thread() {
            public void run() {
                try {
                    processSocket(user);
                } catch (Exception e) {
                    deleteSocket(user, e);
                }
            }
        };
        
        users.add(user);
        user.thread.start();
        
        connected(user);
    }
    
    private void deleteSocket(User user, Exception e) {
        try {
            if (!user.socket.isClosed()) user.socket.close();
            info("Deleting user: " + user.socket.getInetAddress().getHostAddress() + " / " + user.uid + " / " + user.nickname);
        } catch (Exception ex) {}
        users.remove(user);
        
        disconnected(user, e);
    }

    private void processSocket(User user) throws Exception {
        DataInputStream in = new DataInputStream(user.socket.getInputStream());
        DataOutputStream out = new DataOutputStream(user.socket.getOutputStream());
        
        while (!user.socket.isClosed()) {
            processPacket(user, in, out);
        }
        
        deleteSocket(user, null);
    }

    private void processPacket(User user, DataInputStream in, DataOutputStream out) throws Exception {
        byte id = in.readByte();
        gotNewPacket(user, id);
        
        switch (id) {
            case HELLO_PACKET:
                processHello(user, in, out);
                break;
            case UID_REQUEST_PACKET:
                processNicknameRequest(user, in, out);
                break;
            case DISCONNECT_PACKET:
                throw new IOException("User disconnected: " + user.socket.getInetAddress().getHostAddress() + " / " + user.uid + " / " + user.nickname);
            case SEND_MESSAGE_PACKET:
                processSendMessage(user, in, out);
                break;
            default:
                processUnknownPacket(id, out);
                break;
        }
    }

    private void processHello(User user, DataInputStream in, DataOutputStream out) throws Exception {
        byte version = in.readByte();
        sendHelloResult(out);
        if (version != PROTOCOL_VERSION) throw new IOException("Incompatiable protocol version: " + version);
        user.isHandshakeOk = true;
        saidHello(user);
    }

    private void sendHelloResult(DataOutputStream out) throws Exception {
        out.writeByte(HELLO_PACKET);
        out.writeByte(PROTOCOL_VERSION);
    }

    private void processUnknownPacket(byte upid, DataOutputStream out) throws Exception {
        out.writeByte(UNKNOWN_PACKET);
        out.writeByte(upid);
        out.writeByte(PROTOCOL_VERSION);
        throw new IOException("Unknown packet from socket: " + upid);
    }

    private void processNicknameRequest(User user, DataInputStream in, DataOutputStream out) throws Exception {
        if (!user.isHandshakeOk) throw new IOException("User don't said a hello.");
        
        String nickname = in.readUTF();
        long uid;
        
        if (nickname == null || nickname.trim().isEmpty()) uid = -2;
        else if (getByNickname(nickname) != null) uid = -3;
        else uid = nextUID();
        
        sendNicknameRequest(uid, out);
        
        if (uid < 0) throw new IOException("Send UID request error: " + uid);
        else {
            user.uid = uid;
            user.nickname = nickname;
            authorized(user);
        }
    }

    private User getByNickname(String nickname) {
        if (nickname == null) return null;
        
        for (User u : users) {
            if (u != null && u.nickname != null && u.nickname.equals(nickname)) return u;
        }
        
        return null;
    }

    private long nextUID() {
        return nextUID++;
    }
    
    private void sendNicknameRequest(long uid, DataOutputStream out) throws Exception {
        out.writeByte(UID_REQUEST_PACKET);
        out.writeLong(uid);
    }

    private void processSendMessage(User user, DataInputStream in, DataOutputStream out) throws Exception {
        if (user.uid < 0 || user.nickname == null) throw new IOException("User is not authorized.");
        
        String message = in.readUTF();
        if (message == null || message.trim().isEmpty()) return;
        gotNewMessage(user, message);
        sendSendMessages(user, message);
    }

    private void sendSendMessages(User user, String message) {
        for (User u : users) {
            try {
                if (u == null || u.socket == null || u.uid < 0) continue;
                sendSendMessage(user.nickname, message, new DataOutputStream(u.socket.getOutputStream()));
            } catch (Exception e) {
                deleteSocket(u, e);
            }
        }
    }

    private void sendSendMessage(String nickname, String message, DataOutputStream out) throws Exception {
        if (message == null || nickname == null) return;
        
        out.write(SEND_MESSAGE_PACKET);
        out.writeUTF(nickname);
        out.writeUTF(message);
    }

    public void connected(User user) {
        info("Connected " + (user.socket != null ? user.socket.getInetAddress().getHostAddress() : null));
    }

    public void disconnected(User user, Exception e) {
        info("Disconnected " + (user.socket != null ? user.socket.getInetAddress().getHostAddress() : null) + " / " + user.uid + " / " + user.nickname + ", reason: " + e);
    }

    public void gotNewMessage(User user, String message) {
        info("Got a new message " + message + " from " + user.socket.getInetAddress().getHostAddress() + " / " + user.uid + " / " + user.nickname);
    }
    
    public void saidHello(User user) {
        info("User said a hello: " + user.socket.getInetAddress().getHostAddress());
    }
    
    public void authorized(User user) {
        info("Given user id " + user.uid + " to " + user.socket.getInetAddress().getHostAddress() + " / " + user.nickname);
    }
    
    public void gotNewPacket(User user, byte id) {
        info("Got a packet id " + id + " from " + user.socket.getInetAddress().getHostAddress() + " / " + user.uid + " / " + user.nickname);
    }
}
