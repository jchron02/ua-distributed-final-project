import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class UACentralServer {

    public static void main(String[] args) {
        int portNumber = 479;
        try {
            ServerSocket serverSocket = new ServerSocket(portNumber);
            System.out.println("UAServer is running and listening on port " + portNumber +"...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}

class HandleClientInput implements Runnable {
    private Socket clientSocket;
    private Logger logger;

    public HandleClientInput(Socket socket) {
        this.clientSocket = socket;
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
        try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            logInfo("New client connection from IP address " + clientSocket.getInetAddress().getHostAddress());

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received message from client: " + inputLine);
                logInfo("Received message from client: " + inputLine);

                // Start fittingroom task.
            }

            logInfo("Client " + clientSocket.getInetAddress().getHostAddress() + " disconnected");

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            logWarning("Error : " + e.getMessage() + " received from response from client " + clientSocket.getInetAddress().getHostAddress());
        } finally {
            try {
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