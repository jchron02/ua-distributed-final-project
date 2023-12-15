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

    public void clientListener(Socket socket) {
        new Thread(() -> {
            try {
                String line;
                while (clientSocket.isConnected() && (line = clientReader.readLine()) != null) {
                    if (line.startsWith("ARGUMENTS_")) {
                        String[] splitLine = line.split("_");
                        logInfo("Client <" + socket.getInetAddress().getHostAddress() + "> sent over arguments. Sleep Timer - " + splitLine[1] + ". Fitting Rooms to Initialize -  " + splitLine[2]);
                        initializeTotalRooms(splitLine[2]);
                        allowCustomerCreation = true;
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

    public void clientHandler(Socket clientSocket) {
        try {
            clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            clientWriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
            clientListener(clientSocket);

        } catch (IOException e) {
            logError("Error in clientHandler - " + e.getMessage());
        }
    }

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
            systemTimer();
            startCustomers();

        } catch (NumberFormatException e) {
            logError("Error parsing room initialization parameters - " + e.getMessage());
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
            } else if (updateMessage[1].equals("INITIALIZED")) {

            }
        } catch (Exception e) {
            logError("Error updating locks - " + e.getMessage());
        }
    }

    public void systemTimer() {
        new Thread(() -> {
            try {
                Thread.sleep(1000L * systemTime);
                allowCustomerCreation = false;
                logInfo("System time has expired. The Store is close. Customer creation is closed.");
            } catch (InterruptedException e) {
                logError("Error in systemTimer - " + e.getMessage());
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void startCustomers() {
        new Thread(() -> {
            try {
                for (int i = 0; i < numberOfCustomers; i++) {
                    if (allowCustomerCreation) {
                        relayMessageToFittingRoomServer("CUSTOMER_" + (i+1), roundRobin());
                    } else {
                        relayMessageToClient("Customer + " + i + " leaves the store. It's closed.");
                    }
                    Thread.sleep(250); // Random time it takes for a customer to arrive :)
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

    public void relayMessageToFittingRoomServer(String message, int index) {
        try {
            serverWriters.get(index).println(message);
        } catch (Exception e) {
            logError("Error relaying message to fitting room servers - " + e.getMessage());
        }
    }

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

    // Helper methods for logging
    private void logInfo(String message) {
        logger.info(message);
    }

    private void logWarning(String message) {
        logger.warning(message);
    }

    private void logError(String message) {
        logger.severe(message);
    }

    class ServerInfo {
        private int fittingRooms;
        private int waitingRooms;
        private int serverId;
    }
}
