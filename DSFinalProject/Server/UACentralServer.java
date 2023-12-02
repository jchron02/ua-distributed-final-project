import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.*;

public class UACentralServer {

    private static final CopyOnWriteArrayList<ObjectOutputStream> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        int portNumber = 479;
        try {
            ServerSocket serverSocket = new ServerSocket(portNumber);
            System.out.println("UAServer is running and listening on port " + portNumber + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

                // Send confirmation message to the client
                out.writeObject("You are now connected to the server!");

                clients.add(out);

                // Handle client input in a separate thread
                new Thread(new HandleClientInput(clientSocket, out)).start();
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    static class HandleClientInput implements Runnable {
        private Socket clientSocket;
        private ObjectOutputStream clientOut;
        private Logger logger;

        public HandleClientInput(Socket socket, ObjectOutputStream out) {
            this.clientSocket = socket;
            this.clientOut = out;
            configureLogger();
        }

        private void configureLogger() {
            this.logger = Logger.getLogger(HandleClientInput.class.getName());

            try {
                FileHandler fileHandler = new FileHandler("UAServer.log", true);
                fileHandler.setFormatter(new SimpleFormatter());
                logger.addHandler(fileHandler);
            } catch (IOException e) {
                System.err.println("Error setting up logger: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

                logInfo("New client connection from IP address " + clientSocket.getInetAddress().getHostAddress());

                // Handle messages from the client
                while (true) {
                    String clientMessage = (String) in.readObject();
                    System.out.println("Received message from client: " + clientMessage);
                    logInfo("Received message from client: " + clientMessage);

                    // Broadcast the message to all connected clients
                    for (ObjectOutputStream client : clients) {
                        client.writeObject("Client " + clientSocket.getInetAddress().getHostAddress() + ": " + clientMessage);
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error: " + e.getMessage());
                logWarning("Error : " + e.getMessage() + " received from response from client " + clientSocket.getInetAddress().getHostAddress());
            } finally {
                try {
                    clients.remove(clientOut);
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                    logWarning("Error closing client socket: " + e.getMessage());
                }
            }
        }

        private void logInfo(String message) {
            logger.log(Level.INFO, "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] INFO: " + message);
        }

        private void logWarning(String message) {
            logger.log(Level.WARNING, "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] WARNING: " + message);
        }
    }
}
