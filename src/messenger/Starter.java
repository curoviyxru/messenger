package messenger;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author Mathew
 */
public class Starter {
    public static void main(String[] rawArgs) throws Exception {
        List<String> args = Arrays.asList(rawArgs);
        
        if (args.isEmpty()) {
            showInformation();
            return;
        }
        
        switch (args.get(0)) {
            case "client":
                startClient(args);
                break;
            case "server":
                startServer(args);
                break;
            default:
                showInformation();
                break;
        }
        
        System.exit(0);
    }

    private static void showInformation() {
        System.out.println("simpleMessenger, 2020.");
        System.out.println("Client protocol version: " + Client.PROTOCOL_VERSION + ", server: " + Server.PROTOCOL_VERSION);
        System.out.println();
        System.out.println("Usage: messenger <mode>");
        System.out.println();
        System.out.println("Possible modes:");
        System.out.println("client - to start a client instance.");
        System.out.println("server - to start a server instance.");
    }

    private static void startClient(List<String> args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        Client client = new Client();
        System.out.print("Server address: ");
        String address = scanner.nextLine();
        client.connect(address);
        System.out.print("Nickname: ");
        String nickname = scanner.nextLine();
        client.authorize(nickname);

        String line;
        while (!(line = scanner.nextLine()).trim().isEmpty()) {
            client.sendMessage(line);
        }
        
        client.closeConnection();
    }

    private static void startServer(List<String> args) throws Exception {
        Server server = new Server();
        server.start();
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        server.stop();
    }
}
