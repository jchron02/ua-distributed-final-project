import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class UACentralServer {

    public static final int PORT = 479;
    private static final String LOG_FILE = "serverLog.txt";

    private ServerSocket mainServerSocket;
    private Logger logger;

    private ArrayList<Socket> fittingRoomServersList = new ArrayList<>();
    private ArrayList<PrintWriter> printWriters = new ArrayList<>();
    private ArrayList<BufferedReader> bufferedReaders = new ArrayList<>();

    private int roundRobinCounter = 0;
    private int clientCounter = 1;

    public UACentralServer() {
        try {
            setupLogger();

            mainServerSocket = new ServerSocket(PORT);
            logger.info("Main Server listening on port " + PORT);

            acceptFittingRoomServers();

        } catch (IOException e) {
            e.printStackTrace();
            logger.warning("Error in UACentralServer constructor");
        }
    }

    private void setupLogger() {
        try {
            logger = Logger.getLogger(UACentralServer.class.getName());
            FileHandler fileHandler = new FileHandler(LOG_FILE, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            e.printStackTrace();
            logger.warning("Error at setupLogger()");
        }
    }

    private void acceptFittingRoomServers() {
        new Thread(() -> {
            try {
                ServerSocket fittingRoomServerSocket = new ServerSocket(252); // Dynamic port
                logger.info("Fitting Room Server listening on port " + fittingRoomServerSocket.getLocalPort());

                while (true) {
                    Socket fittingRoomServer = fittingRoomServerSocket.accept();
                    fittingRoomServersList.add(fittingRoomServer);
                    PrintWriter out = new PrintWriter(fittingRoomServer.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(fittingRoomServer.getInputStream()));
                    printWriters.add(out);
                    bufferedReaders.add(in);

                    logger.info("New fitting room server connection from IP address " +
                            fittingRoomServer.getInetAddress().getHostAddress());
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                logger.warning("Error at accepting fitting room servers");
            }
        }).start();
    }

    private void clientHandler(Socket clientSocket) {
        int balance = loadBalancer();
        new Thread(() -> fittingRoomServerHandler(fittingRoomServersList.get(balance), balance, clientSocket)).start();
        logger.info("Client " + clientSocket.getInetAddress().getHostAddress() + " is being balanced");
    }

    private void fittingRoomServerHandler(Socket fittingRoomServer, int servNum, Socket clientSocket) {
        try {
            PrintWriter pw = printWriters.get(servNum);
            pw.println(clientCounter++);
            logger.info("Current State of Client Counter " + clientCounter);

            // Forward the client socket details to the fitting room server
            pw.println("NEW_CUSTOMER");
            pw.println(clientSocket.getInetAddress().getHostAddress());
            pw.println(clientSocket.getPort());
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.warning("Error occurred on fittingRoomServerHandler() from server " + servNum +
                    " located at " + fittingRoomServer.getInetAddress().getHostAddress());
        }
    }

    private int loadBalancer() {
        if (fittingRoomServersList.isEmpty()) {
            logger.warning("No fitting room servers available");
            return -1; // Handle appropriately in your application
        }

        roundRobinCounter = (roundRobinCounter + 1) % fittingRoomServersList.size();
        return roundRobinCounter;
    }

    public void start() {
        while (true) {
            try {
                Socket clientSocket = mainServerSocket.accept();
                logger.info("New client connection from IP address " + clientSocket.getInetAddress().getHostAddress());
                new Thread(() -> clientHandler(clientSocket)).start();
            } catch (IOException e) {
                e.printStackTrace();
                logger.warning("Error at start() method on the clientSocket");
            }
        }
    }

    public static void main(String[] args) {
        UACentralServer centralServer = new UACentralServer();
        centralServer.start();
    }
}
