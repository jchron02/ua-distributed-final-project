import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class FittingRoomServer {
    private static Semaphore fittingRoomLock;
    private static Semaphore waitingRoomLock;

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
            while (true) {
                // Receive a message from UACentralServer
                Object centralServerMessage = in.readObject();
                System.out.println("Received from UACentralServer: " + centralServerMessage);

                // Process the message
                if (centralServerMessage instanceof String && ((String) centralServerMessage).startsWith("REQUESTING")) {
                    // Handle permits information message
                    updatePermits(out);
                } else if (centralServerMessage instanceof String && ((String) centralServerMessage).startsWith("NEW")) {
                    // Handle customer request
                    handleCustomerRequest((String) centralServerMessage);
                } else if (centralServerMessage instanceof String && ((String) centralServerMessage).startsWith("FITTING")) {
                    // Handle fitting room request
                    handleFittingRoomRequest((String) centralServerMessage);

                }
                // Add more cases if needed for other types of messages

                // Perform other tasks as needed

            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void updatePermits(ObjectOutputStream out) {
        // Send available permits to UACentralServer
        String permitsMessage = "PermitsInfo#" + fittingRoomLock.availablePermits() + "#" + waitingRoomLock.availablePermits();
        System.out.println("Sending to UACentralServer: " + permitsMessage);
        try {
            out.writeObject(permitsMessage);
            out.flush();
        } catch (IOException e) {
            System.out.println("Error sending message to UACentralServer: " + e.getMessage());
        }
    }

    private static void handleCustomerRequest(String customerRequest) {
        // Parse the customer request and handle it
        String[] parts = customerRequest.split("#");
        if (parts.length == 2 && "Customer".equals(parts[0])) {
            int customerId = Integer.parseInt(parts[1]);
            // Handle the customer request as needed
            System.out.println("Received customer " + customerId + " in FittingRoomServer");
        }
    }

    private static void handleFittingRoomRequest(String fittingRoomRequest) {
        String[] parts = fittingRoomRequest.split("#");
        if (parts.length == 4 && "FITTING_ROOM".equals(parts[0]) && "WAITING_ROOM".equals(parts[2])) {
            int numFittingRooms = Integer.parseInt(parts[1]);
            int numWaitingRooms = Integer.parseInt(parts[3]);
            // Handle the customer request as needed
            System.out.println("Instantiating FittingRooms " + numFittingRooms + " and WaitingRooms " + numWaitingRooms + " in FittingRoomServer");

            fittingRoomLock = new Semaphore(numFittingRooms);
            waitingRoomLock = new Semaphore(numWaitingRooms);
        }
    }
}