import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class UAClient {

    private static final String CENTRAL_SERVER_IP = "localhost";
    private static final int CENTRAL_SERVER_PORT = 479;

    public static void main(String[] args) {
        try (Socket socket = new Socket(CENTRAL_SERVER_IP, CENTRAL_SERVER_PORT);
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // Display the confirmation message from the server
            String confirmationMessage = (String) in.readObject();
            System.out.println("Server says: " + confirmationMessage);

            // Keep the client connected and waiting for server messages
            while (true) {
                String serverMessage = (String) in.readObject();
                if (serverMessage != null) {
                    System.out.println("Server says: " + serverMessage);
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Unable to connect to server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
