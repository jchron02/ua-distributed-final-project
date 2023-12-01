import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

/********************************
 Name: Joshua C, David T, Christopher M
 Team: 3
 Final Project
 Due Date: 
 ********************************/
public class UAClient {

    
    private static final String CENTRAL_SERVER_IP = "localhost";
    private static final int CENTRAL_SERVER_PORT = 479;

    public static void main(String[] args) {
        try (Socket socket = new Socket(CENTRAL_SERVER_IP, CENTRAL_SERVER_PORT);
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // Implement logic for printing out the messages it receives from UACentralServer and FittingRoomServer

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}