import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class FittingRoomServer {
    public static void main(String[] args) {
        String centralServerAddress = "localhost";
        int centralServerPort = 5555;

        try (Socket socket = new Socket(centralServerAddress, centralServerPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // Register with the UACentralServer as a fitting room server
            out.writeObject("FITTING_ROOM_SERVER");
            out.flush();

            // Now the server is registered and can send/receive messages through UACentralServer
            // For example, receive a message from UACentralServer
            Object centralServerMessage = in.readObject();
            System.out.println("Received from UACentralServer: " + centralServerMessage);

            // Send a message to UACentralServer
            Object serverOutput = "Hello from FittingRoomServer";
            out.writeObject(serverOutput);
            out.flush();
            System.out.println("Sent to UACentralServer: " + serverOutput);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}