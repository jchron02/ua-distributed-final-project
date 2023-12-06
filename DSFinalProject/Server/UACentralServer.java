import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class UACentralServer {
    private static final List<ObjectOutputStream> clientOutputStreams = new ArrayList<>();
    private static final List<ObjectOutputStream> fittingRoomServerStreams = new ArrayList<>();

    public static void main(String[] args) {
        int portNumber = 5555;

        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("UACentralServer is running and waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connected to a client");

                // Handle the client connection in a separate thread
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (
                    ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())
            ) {
                // Read the client type
                Object clientType = in.readObject();

                if ("UACLIENT".equals(clientType)) {
                    // Handle communication with UAClient
                    handleUAClientCommunication(in, out);
                } else if ("FITTING_ROOM_SERVER".equals(clientType)) {
                    // Handle communication with FittingRoomServer
                    handleFittingRoomServerCommunication(in, out);
                } else {
                    System.out.println("Unknown client type");
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private void handleUAClientCommunication(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
            // Register UAClient
            clientOutputStreams.add(out);

            // Read and process objects from the UAClient
            Object clientInput = in.readObject();
            System.out.println("Received from UAClient: " + clientInput);

            // Respond to the UAClient
            Object serverResponse = "Server response to UAClient";
            out.writeObject(serverResponse);
            out.flush();
            System.out.println("Sent response to UAClient");
        }

        private void handleFittingRoomServerCommunication(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
            // Communicate with FittingRoomServer
            fittingRoomServerStreams.add(out);
            // For example, send a message to FittingRoomServer
            Object messageToFittingRoom = "Hello from UACentralServer to FittingRoomServer";
            out.writeObject(messageToFittingRoom);
            out.flush();
            System.out.println("Sent message to FittingRoomServer: " + messageToFittingRoom);

            // Receive a message from FittingRoomServer
            Object fittingRoomResponse = in.readObject();
            System.out.println("Received from FittingRoomServer: " + fittingRoomResponse);

            // Relay the message to all connected UAClients
            relayMessageToAllUAClients(fittingRoomResponse);
        }

        private void relayMessageToAllUAClients(Object message) {
            for (ObjectOutputStream clientOut : clientOutputStreams) {
                try {
                    clientOut.writeObject(message);
                    clientOut.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void relayMessageToAllFittingRoomServers(Object message) {
            for (ObjectOutputStream fitOut : fittingRoomServerStreams) {
                try {
                    fitOut.writeObject(message);
                    fitOut.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void relayMessageToUAClient(Object message, ObjectOutputStream out) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        private void relayMessageToFittingRoomServer(Object message, ObjectOutputStream out) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}