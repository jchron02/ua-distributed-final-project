import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class UAClient {
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 5555;

        int sleepTimer = Integer.parseInt(args[0]);
        int numberOfFittingRooms = Integer.parseInt(args[1]);

        try (Socket socket = new Socket(serverAddress, serverPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // Send an object to the server
            Object clientOutput = "UACLIENT";
            out.writeObject(clientOutput);
            out.flush();

            // Send sleep timer and number of fitting rooms
            out.writeInt(sleepTimer);
            out.flush();
            out.writeInt(numberOfFittingRooms);
            out.flush();

//            Unable to verify this worked... OBSOLETE
//            while (true) {
//                // Receive a message from UACentralServer
//                try {
//                    Object centralServerMessage = in.readObject();
//
//                    // Perform other tasks as needed
//                } catch (IOException | ClassNotFoundException e) {
//                }
//
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}