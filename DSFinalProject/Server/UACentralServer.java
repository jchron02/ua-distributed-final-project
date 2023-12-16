import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class UACentralServer {
    private static final int PORT = 479;
    private static final String LOG_FILE = "CentralServerLog.log";
    private BufferedReader clientReader;
    private PrintWriter clientWriter;
    private Socket clientSocket;
    private int generatedServerId = 0;
    private ServerSocket mainServerSocket;
    private Logger logger;
    private int fittingRooms;
    private int waitingRooms;
    private int trackingFittingRooms = 0;
    private int trackingWaitingRooms = 0;
    private int numberOfCustomers;
    private int accountedCustomers = 0;
    private final CountDownLatch closeConnectionToClient = new CountDownLatch(1);
    private int lastUsedServerIndex = -1;
    private int systemTime;
    private boolean allowCustomerCreation = true;
    private ArrayList<Socket> fittingRoomServersList = new ArrayList<>();
    private ArrayList<PrintWriter> serverWriters = new ArrayList<>();
    private ArrayList<BufferedReader> serverReaders = new ArrayList<>();
    private ArrayList<ServerInfo> serverInfoList = new ArrayList<>();

    /**
     * Creates a new UACentralServer.
     *
     */
    public UACentralServer() {
        try {
            setupLogger();
            acceptClients();
            acceptFittingRoomServers();
        } catch (Exception e) {
            logError("Error in UACentralServer constructor - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sets up the logging system for UACentralServer.
     *
     *
     *
     */
    public void setupLogger() {
        try {
            logger = Logger.getLogger(UACentralServer.class.getName());
            FileHandler fileHandler = new FileHandler(LOG_FILE, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            logError("Error at setupLogger() - " + e.getMessage());
        }
    }

    /**
     * Accepts a client connection and starts client handling.
     *
     *
     *
     */
    public void acceptClients() {
        new Thread(() -> {
            try {
                mainServerSocket = new ServerSocket(PORT);
                logInfo("Main Server listening on port - " + PORT);
                while (true) {
                    clientSocket = mainServerSocket.accept();
                    logInfo("New client connection from IP address <" + clientSocket.getInetAddress().getHostAddress() + ">");
                    clientHandler(clientSocket);
                }
            } catch (IOException ex) {
                logWarning("Error during accepting clients - " + ex.getMessage());
            }
        }).start();
    }

    /**
     * Accepts a fitting room server connections and starts fitting room server handling.
     *
     *
     *
     */
    public void acceptFittingRoomServers() {
        new Thread(() -> {
            try {
                ServerSocket fittingRoomServerSocket = new ServerSocket(252);
                logInfo("Fitting Room Server listening on port - " + fittingRoomServerSocket.getLocalPort());
                while (true) {
                    Socket fittingRoomServer = fittingRoomServerSocket.accept();
                    logInfo("New fitting room server connection from IP address <" + fittingRoomServer.getInetAddress().getHostAddress() + ">");
                    fittingRoomServersList.add(fittingRoomServer);
                    serverListener(fittingRoomServer);
                }
            } catch (IOException ex) {
                logWarning("Error at accepting fitting room servers - " + ex.getMessage());
            }
        }).start();
    }

    /**
     * Checks if the client has sent arguments to UACentralServer or has disconnected.
     * @param socket
     *
     *
     */
    public void clientListener(Socket socket) {
        new Thread(() -> {
            try {
                String line;
                while (clientSocket.isConnected() && (line = clientReader.readLine()) != null) {
                    if (line.startsWith("ARGUMENTS_")) {
                        String[] splitLine = line.split("_");
                        logInfo("Client <" + socket.getInetAddress().getHostAddress() + "> sent over arguments. Sleep Timer - " + splitLine[1] + ". Fitting Rooms to Initialize -  " + splitLine[2]);
                        initializeTotalRooms(splitLine[2]);
                        systemTime = Integer.parseInt(splitLine[1]);
                    } else if (line.startsWith("DISCONNECTED_")) {
                        logInfo("Client <" + socket.getInetAddress().getHostAddress() + "> disconnected");
                        break;
                    } else {
                        logInfo("Client <" + socket.getInetAddress().getHostAddress() + "> sent over unknown command - " + line);
                    }
                }
                clientWriter.close();
                clientReader.close();
                clientSocket.close();
            } catch (IOException e) {
                logError("Error in clientListener - " + e.getMessage());
                throw new RuntimeException(e);

    public void acceptFittingRoomServers() {
        new Thread(() -> {
            try {
                ServerSocket fittingRoomServerSocket = new ServerSocket(252);
                logInfo("Fitting Room Server listening on port - " + fittingRoomServerSocket.getLocalPort());
                while (true) {
                    Socket fittingRoomServer = fittingRoomServerSocket.accept();
                    logInfo("New fitting room server connection from IP address <" + fittingRoomServer.getInetAddress().getHostAddress() + ">");
                    fittingRoomServersList.add(fittingRoomServer);
                    serverListener(fittingRoomServer);
                }
            } catch (IOException ex) {
                logWarning("Error at accepting fitting room servers - " + ex.getMessage());
            }
        }).start();
    }
        
    public void serverListener(Socket socket) {
        new Thread(() -> {
            try {
                BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter serverWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                serverWriters.add(serverWriter);
                serverReaders.add(serverReader);
                serverWriter.println("ID_" + ++generatedServerId);
                ServerInfo serverInfo = new ServerInfo();
                serverInfoList.add(serverInfo);
                serverInfo.serverId = generatedServerId;
                String line;
                while (socket.isConnected() && (line = serverReader.readLine()) != null) {
                    if (line.startsWith("RELAY_")) {
                        String[] splitLine = line.split("_");
                        relayMessageToClient(splitLine[1]);
                    } else if (line.startsWith("UPDATELOCKS")) {
                        String[] splitLine = line.split("_");
                        updateLocks(splitLine, serverInfo);
                        logInfo("New lock information given from Fitting Room Server <" + socket.getInetAddress().getHostAddress() + ">  Total Fitting Rooms available - " + trackingFittingRooms + ". Total Waiting Rooms available - " + trackingWaitingRooms);

                    } else if (line.startsWith("DISCONNECT_")) {
                        break;
                    }else{
                        logWarning("Server <" + socket.getInetAddress().getHostAddress() + "> sent over unknown command - " + line);
                    }
                }
                socket.close();
                serverWriter.close();
                serverReader.close();
                serverWriters.remove(serverWriter);
                serverReaders.remove(serverReader);
                fittingRoomServersList.remove(socket);

                    } else {
                        logWarning("Server <" + socket.getInetAddress().getHostAddress() + "> sent over unknown command - " + line);
                    }
                }

            } catch (IOException e) {
                logError("Error in serverListener - " + e.getMessage());
                throw new RuntimeException(e);
            }
        }).start();
    }


    /**
     * Creates and output stream and input stream for a client connection.
     * @param clientSocket
     *
     *
     */
    public void clientHandler(Socket clientSocket) {
        try {
            clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            clientWriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
            clientListener(clientSocket);

        } catch (IOException e) {
            logError("Error in clientHandler - " + e.getMessage());
        }
    }

    /**
     * Initializes the total number of fitting rooms and waiting chairs.
     * @param rooms
     * @throws NumberFormatException
     *
     */
    public void initializeTotalRooms(String rooms) {
        try {
            fittingRooms = Integer.parseInt(rooms);
            waitingRooms = fittingRooms * 2;
            trackingFittingRooms += fittingRooms;
            trackingWaitingRooms += waitingRooms;
            numberOfCustomers = fittingRooms + waitingRooms;
            int fittingRoomRemainder = fittingRooms % fittingRoomServersList.size();
            int waitingRoomRemainder = waitingRooms % fittingRoomServersList.size();
            int fittingRoomsPerServer = fittingRooms / fittingRoomServersList.size();
            int waitingRoomPerServer = waitingRooms / fittingRoomServersList.size();

            for (int i = 0; i < fittingRoomServersList.size() - 1; i++) {
                initializeFittingRooms("INITIALIZE_" + fittingRoomsPerServer + "_" + waitingRoomPerServer, i);
                serverInfoList.get(i).fittingRooms = fittingRoomsPerServer;
                serverInfoList.get(i).waitingRooms = waitingRoomPerServer;
            }
            initializeFittingRooms("INITIALIZE_" + (fittingRoomsPerServer + fittingRoomRemainder) + "_" + (waitingRoomPerServer + waitingRoomRemainder), fittingRoomServersList.size() - 1);
            serverInfoList.get(fittingRoomServersList.size() - 1).fittingRooms = (fittingRoomsPerServer + fittingRoomRemainder);
            serverInfoList.get(fittingRoomServersList.size() - 1).waitingRooms = (fittingRoomsPerServer + fittingRoomRemainder);
//            systemTimer();
            startCustomers();

        } catch (NumberFormatException e) {
            logError("Error parsing room initialization parameters - " + e.getMessage());
        }
    }

    /**
     * Calls a method to relay initialization of fitting rooms to the fitting room server.
     * @param message
     * @param index
     *
     *
     */
    public void initializeFittingRooms(String message, int index) {

        relayMessageToFittingRoomServer(message, index);
    }

    /**
     * Allocates where the customer should go based on a round-robin system.
     *
     * @throws RuntimeException
     * @return index
     */
    public int roundRobin() {
        if (fittingRoomServersList.size() > 1) {
            if (lastUsedServerIndex == 0) {
                lastUsedServerIndex = 1;
            } else if (lastUsedServerIndex == fittingRoomServersList.size() - 1) {
                lastUsedServerIndex = 0;
            } else {
                lastUsedServerIndex++;
            }
        } else {
            lastUsedServerIndex = 0;
        }
        return lastUsedServerIndex;
    }

    /**
     * Updates the locks for the waiting rooms and fitting rooms.
     * @param updateMessage
     * @param serverInfo
     *
     *
     */
    public void updateLocks(String[] updateMessage, ServerInfo serverInfo) {
        try {
            if (updateMessage[1].equals("WAITING")) {
                if (updateMessage[2].equals("ACQUIRE")) {
                    trackingWaitingRooms--;
                    serverInfo.waitingRooms--;
                } else {
                    trackingWaitingRooms++;
                    serverInfo.waitingRooms++;
                }
            } else if (updateMessage[1].equals("FITTING")) {
                if (updateMessage[2].equals("ACQUIRE")) {
                    trackingFittingRooms--;
                    serverInfo.fittingRooms--;
                } else {
                    trackingFittingRooms++;
                    serverInfo.fittingRooms++;
                }
            }
    }

    public void initializeFittingRooms(String message, int index) {

        relayMessageToFittingRoomServer(message, index);
    }

    public int roundRobin() {
        int index = -1;
        for (int i = (lastUsedServerIndex + 1) % serverInfoList.size(); i != lastUsedServerIndex; i = (i + 1) % serverInfoList.size()) {
            ServerInfo serverInfo = serverInfoList.get(i);

            if (serverInfo.fittingRooms > 0) {
                index = i;
                break;
            }else if(serverInfo.waitingRooms > 0){
                index = i;
                break;
            }
        }
        lastUsedServerIndex = index;

        return index;
    }

    public void updateLocks(String[] updateMessage, ServerInfo serverInfo) {
        try {
            if (updateMessage[1].equals("WAITING")) {
                if (updateMessage[2].equals("ACQUIRE")) {
                    trackingWaitingRooms--;
                    serverInfo.waitingRooms--;
                } else {
                    trackingWaitingRooms++;
                    serverInfo.waitingRooms++;
                }
            } else if (updateMessage[1].equals("FITTING")) {
                if (updateMessage[2].equals("ACQUIRE")) {
                    trackingFittingRooms--;
                    serverInfo.fittingRooms--;
                } else {
                    trackingFittingRooms++;
                    serverInfo.fittingRooms++;
                }
            }
        } catch (Exception e) {
            logError("Error updating locks - " + e.getMessage());
        }
    }

    /**
     * Creates the amount of time until the system shuts down.
     *
     * @throws RuntimeException
     *
     */
//    public void systemTimer() {
//        new Thread(() -> {
//            try {
//                Thread.sleep(1000L * systemTime);
//                allowCustomerCreation = false;
//                logInfo("System time has expired. The Store is close. Customer creation is closed.");
//            } catch (InterruptedException e) {
//                logError("Error in systemTimer - " + e.getMessage());
//                throw new RuntimeException(e);
//            }
//        }).start();
//    }

    /**
     * Creates the customers or leaves an update on the status of the store.
     *
     *
     *
     */
    public void startCustomers() {
        new Thread(() -> {
            try {
                for (int i = 0; i < numberOfCustomers; i++) {
                    if (allowCustomerCreation) {
                        relayMessageToFittingRoomServer("CUSTOMER_" + (i+1), roundRobin());
                    } else {
                        relayMessageToClient("Customer + " + i + " leaves the store. It's closed.");
                    }
                    Thread.sleep(1000); // Random time it takes for a customer to arrive :)
                }
                closeConnectionToClient.await();
                clientWriter.println("ACCOUNTED_THANKS FOR SHOPPING");
                clientReader.close();
                clientWriter.close();
                clientSocket.close();
            } catch (Exception e) {
                logError("Error in startCustomers - " + e.getMessage());
            }
        }).start();
    }


    /**
     * Relays a message to all fitting room servers.
     * @param message
     * @param index
     * @throws RuntimeException
     *
     */
    public void relayMessageToFittingRoomServer(String message, int index) {
        try {
            serverWriters.get(index).println(message);
        } catch (Exception e) {
            logError("Error relaying message to fitting room servers - " + e.getMessage());
        }
    }

    /**
     * Relays a message to all client servers.
     * @param message
     *
     *
     */
    public void relayMessageToClient(String message) {
        try {
            clientWriter.println(message);
            if (message.toLowerCase().contains("leaves")) {
                accountedCustomers++;
            }
            if (accountedCustomers == numberOfCustomers) {
                closeConnectionToClient.countDown();
            }
        } catch (Exception e) {
            logError("Error relaying message to client - " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new UACentralServer();
    }

    private void logInfo(String message) {
        logger.info(message);
    }

    /**
     * Displays log info.
     * @param message
     *
     *
     */
    private void logInfo(String message) {
        logger.info(message);
    }

    /**
     * Displays a log warning.
     * @param message
     *
     *
     */
    private void logWarning(String message) {
        logger.warning(message);
    }
 
    /**
     * Displays a log error.
     * @param message
     *
     *
     */
    private void logError(String message) {
        logger.severe(message);
    }

    class ServerInfo {
        private int fittingRooms;
        private int waitingRooms;
        private int serverId;
    }
}
