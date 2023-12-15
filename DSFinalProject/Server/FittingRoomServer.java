import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class FittingRoomServer {

    private static final String HOST = "localhost";
    private static final int PORT = 252;
    private static final String LOG_FILE = "FittingRoomServerLog.log";
    private Socket centralServerSocket;
    private BufferedReader centralServerIn;
    private PrintWriter centralServerOut;
    private int fittingRooms;
    private int waitingSeats;
    private int waitingCount = 0;
    private int numRooms;
    private int numSeats;
    private Semaphore seatController;
    private Semaphore roomController;
    private int serverId;

    // Logger setup
    private static final Logger logger = Logger.getLogger(FittingRoomServer.class.getName());
    private FileHandler fileHandler;

    public FittingRoomServer() {
        try {
            // Set up logger
            fileHandler = new FileHandler(LOG_FILE, true);
            logger.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);

            // Connect to CentralServer
            centralServerSocket = new Socket(HOST, PORT);
            centralServerIn = new BufferedReader(new InputStreamReader(centralServerSocket.getInputStream()));
            centralServerOut = new PrintWriter(new OutputStreamWriter(centralServerSocket.getOutputStream()), true);

            // Register with CentralServer
            centralServerOut.println("FITTING_ROOM");
            serverListener(centralServerSocket);

        } catch (IOException | SecurityException e) {
            logError("Error during server initialization " + e.getMessage());
        }
    }

    public void serverListener(Socket socket) {
        new Thread(() -> {
            try {
                String line;
                while (socket.isConnected() && (line = centralServerIn.readLine()) != null) {
                    if (line.startsWith("INITIALIZE_")) {
                        String[] splitLine = line.split("_");
                        initializeRooms(splitLine[1], splitLine[2]);
                    } else if (line.startsWith("CUSTOMER_")) {
                        acceptCustomers(line.split("_"));
                    } else if (line.startsWith("ID_")) {
                        String[] splitLine = line.split("_");
                        serverId = Integer.parseInt(splitLine[1]);
                    } else {
                        logWarning("Unexpected Command - " + line);
                        System.out.println("Unexpected Command - " + line);
                    }
                }
            } catch (IOException e) {
                logError("Error in server listener - " + e.getMessage());
            }
        }).start();
    }

    public void initializeRooms(String numRooms, String numSeats) {
        try {
            fittingRooms = Integer.parseInt(numRooms);
            waitingSeats = Integer.parseInt(numSeats);
            roomController = new Semaphore(fittingRooms);
            seatController = new Semaphore(waitingSeats);
            updateLocks("INITIALIZED", "LETSGOOOOO");
            logInfo("Rooms initialized - Fitting Rooms: " + fittingRooms + ", Waiting Seats: " + waitingSeats);
        } catch (NumberFormatException e) {
            logError("Error parsing room initialization parameters - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void acceptCustomers(String[] customerInfo) {
        try {
            int customerID = Integer.parseInt(customerInfo[1]);
            Customer c = new Customer(customerID, this);
            c.start();
        } catch (NumberFormatException e) {
            logError("Error parsing customer information - " + e.getMessage());
        }
    }

    public void enterFitting(int customerID) {
        try {
            roomController.acquire();
            updateLocks("FITTING", "ACQUIRE");
            leaveWaiting();
            logInfo("\t\tCustomer #" + customerID + " enters Fitting Room located at <Server " + serverId + ": " + InetAddress.getLocalHost() + ">");
            sendMessageToClient("\t\tCustomer #" + customerID + " enters Fitting Room located at <Server " + serverId + ": " + InetAddress.getLocalHost() + ">");
            Thread.sleep(new Random().nextInt(1000));
            logInfo("\t\tWe have " + (waitingSeats - seatController.availablePermits()) + " waiting and " + (fittingRooms - roomController.availablePermits()) + " changing");
            sendMessageToClient("\t\tWe have " + (waitingSeats - seatController.availablePermits()) + " waiting and " + (fittingRooms - roomController.availablePermits()) + " changing");
        } catch (InterruptedException | IOException e) {
            logError("Error during entering fitting room - "+ e.getMessage());
            e.printStackTrace();
        }
    }

    public void leaveFitting(int customerID) {
        try {
            roomController.release();
            updateLocks("FITTING", "RELEASE");
            logInfo("\t\t\tCustomer #" + customerID + " leaves the Fitting Room located at <Server " + serverId + ": " + InetAddress.getLocalHost() + ">");
            sendMessageToClient("\t\t\tCustomer #" + customerID + " leaves the Fitting Room located at <Server " + serverId + ": " + InetAddress.getLocalHost() + ">");
            logInfo("\t\tWe have " + (waitingSeats - seatController.availablePermits()) + " waiting and " + (fittingRooms - roomController.availablePermits()) + " changing");
            sendMessageToClient("\t\tWe have " + (waitingSeats - seatController.availablePermits()) + " waiting and " + (fittingRooms - roomController.availablePermits()) + " changing");
        } catch (IOException e) {
            logError("Error during leaving fitting room - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void enterWaiting(int customerID) {
        try {
            if (seatController.tryAcquire()) {
                updateLocks("WAITING", "ACQUIRE");
                logInfo("\tCustomer #" + customerID + " enters the waiting area on <Server " + serverId + ": " + InetAddress.getLocalHost() + "> and has a seat.");
                sendMessageToClient("\tCustomer #" + customerID + " enters the waiting area on <Server " + serverId + ": " + InetAddress.getLocalHost() + "> and has a seat.");

                logInfo("\tWe have " + (waitingSeats - seatController.availablePermits()) + " waiting on <Server " + serverId + ": " + InetAddress.getLocalHost());
                sendMessageToClient("\tWe have " + (waitingSeats - seatController.availablePermits()) + " waiting on <Server " + serverId + ": " + InetAddress.getLocalHost());
            } else {
                logInfo("\tCustomer #" + customerID + " could not find a seat and leaves in frustration.");
                sendMessageToClient("\tCustomer #" + customerID + " could not find a seat and leaves in frustration.");
            }
        } catch (IOException e) {
            logError("Error during entering waiting area - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void leaveWaiting() {
        seatController.release();
        updateLocks("WAITING", "RELEASE");
    }

    public void updateLocks(String type, String action) {
        centralServerOut.println("UPDATELOCKS_" + type + "_" + action);
    }

    public void sendMessageToClient(String message) {
        centralServerOut.println("RELAY_" + message);
    }

    class Customer extends Thread {
        int customerID;
        FittingRoomServer fit;

        public Customer(int customerID, FittingRoomServer fit) {
            this.customerID = customerID;
            this.fit = fit;
        }

        @Override
        public void run() {
            fit.enterWaiting(customerID);
            fit.enterFitting(customerID);
            fit.leaveFitting(customerID);
        }
    }

    private void logInfo(String message) {
        logger.info(message);
    }

    private void logWarning(String message) {
        logger.warning(message);
    }

    private void logError(String message) {
        logger.severe(message);
    }

    public static void main(String[] args) {
        new FittingRoomServer();
    }
}
