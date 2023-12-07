import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;


public class UACentralServer {

    private static final List<ObjectOutputStream> clientOutputStreams = new ArrayList<>();
    private static final List<ObjectOutputStream> fittingRoomServerOutputStreams = new ArrayList<>();
    private static final List<ObjectInputStream> fittingRoomServerInputStreams = new ArrayList<>();
    private static AtomicInteger sleepTimer = new AtomicInteger(0);
    private static AtomicInteger numberOfFittingRooms = new AtomicInteger(0);
    private static AtomicInteger numberOfWaitingRooms = new AtomicInteger(0);
    private static AtomicInteger fittingRoomPermits = new AtomicInteger(0);
    private static AtomicInteger waitingChairPermits = new AtomicInteger(0);
    private static AtomicInteger customerIdCounter = new AtomicInteger(0);
    private static int numberOfCustomers = 0;
    private static int fittingRoomIndex = 0;
    private static CountDownLatch customersLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        int portNumber = 5555;

        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("UACentralServer is running and waiting for clients...");

            new Thread(() -> {
                try {
                    // Wait for numberOfCustomers to be populated
                    customersLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                for (int i = 0; i < numberOfCustomers; i++) {
                    new Thread(() -> communicateWithFittingRoomServers()).start();
                }
            }).start();

            while (true) {
                Socket connectionSocket = serverSocket.accept();
                System.out.println("Connection Received.");

                // Handle the client connection in a separate thread
                ConnectionHandler connectionHandler = new ConnectionHandler(connectionSocket);
                new Thread(connectionHandler).start();


            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses the permits message and updates permits.
     * @param permitsMessage
     * @throws RuntimeException
     * @return String Array
     */

    private static String[] getPermitValues(String permitsMessage) {
        // Parse the permits message and update permits
        String[] parts = permitsMessage.split("#");
        if (parts.length == 3 && "PermitsInfo".equals(parts[0])) {
            return parts;
        } else {
            throw new RuntimeException("Invalid permits message format");
        }

    }

    /**
     * Creates a new customer.
     * @param type
     * @return void
     */
    private static void createNewCustomer(String type) {
        if (type.equals("FITTING_ROOM")) {
            ObjectOutputStream fittingRoomServerOut = fittingRoomServerOutputStreams.get(0);
            handleCustomerRequest(fittingRoomServerOut);
        } else if (type.equals("WAITING")) {
            ObjectOutputStream waitingRoomServerOut = fittingRoomServerOutputStreams.get(0);
            handleCustomerRequest(waitingRoomServerOut);
        }
    }

    /**
     * Handles customer requests assuming that the customer request is sent to FittingRoomServers
     * @param fittingRoomServerOut
     *
     * @return void
     */
    private static void handleCustomerRequest(ObjectOutputStream fittingRoomServerOut) {
        // Assuming the customer request is sent to FittingRoomServers
        // Generate a new customer ID
        int customerId = customerIdCounter.incrementAndGet();

        // Check permits and send the customer to the fitting room if available
        if (fittingRoomPermits.get() > 0) {
            sendCustomerToFittingRoom(customerId, fittingRoomServerOut);
        } else {
            // Handle the case when no fitting rooms are available
            System.out.println("No fitting rooms available for customer " + customerId);
        }
    }

    /**
     * Sends the customer ID to the FittingRoomServer
     * @param customerId
     * @param fittingRoomServerOut
     * @throws RuntimeException
     * @return String Array
     */
    private static void sendCustomerToFittingRoom(int customerId, ObjectOutputStream fittingRoomServerOut) {
        try {
            // Send the customer ID to the FittingRoomServer
            fittingRoomServerOut.writeObject("Customer#" + customerId);
            fittingRoomServerOut.flush();
            System.out.println("Sent customer " + customerId + " to FittingRoomServer" + fittingRoomIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to all FittingRoomServers with permits information.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     * @return void
     */
    private static void communicateWithFittingRoomServers() {
        // Send a message to all FittingRoomServers with permits information
        int fittingRoomPermitsCheck = 0;
        int waitingChairPermitsCheck = 0;
        Object permitsMessage = "REQUESTING_PERMITS";

        try {
            ObjectOutputStream fittingRoomServerOut = fittingRoomServerOutputStreams.get(0);
            ObjectInputStream fittingRoomServerIn = fittingRoomServerInputStreams.get(0);
            fittingRoomServerOut.writeObject(permitsMessage);
            fittingRoomServerOut.flush();

            Object responseMessage = fittingRoomServerIn.readObject();
            String[] parts = getPermitValues((String) responseMessage);
            int numFittingRooms = Integer.parseInt(parts[1]);
            int numWaitingChairs = Integer.parseInt(parts[2]);
            fittingRoomPermitsCheck = numFittingRooms;
            waitingChairPermitsCheck = numWaitingChairs;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        fittingRoomPermits.set(fittingRoomPermitsCheck);
        waitingChairPermits.set(waitingChairPermitsCheck);


        createNewCustomer("FITTING");
    }

    private static class ConnectionHandler implements Runnable {
        private final Socket connectionSocket;

        public ConnectionHandler(Socket connectionSocket) {
            this.connectionSocket = connectionSocket;
        }

        /**
         * Reads in a connection to determine whether it is UAClient or FittingRoomServer.
         * @param
         * @throws IOException
         * @throws ClassNotFoundException
         * @return void
         */
        @Override
        public void run() {
            try (
                    ObjectOutputStream out = new ObjectOutputStream(connectionSocket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(connectionSocket.getInputStream())
            ) {
                // Read the connection type
                Object connectionType = in.readObject();

                if ("UACLIENT".equals(connectionType)) {
                    // Handle communication with UAClient
                    handleUAClientCommunication(in, out);
                } else if ("FITTING_ROOM_SERVER".equals(connectionType)) {
                    // Handle communication with FittingRoomServer
                    handleFittingRoomServerCommunication(in, out);
                } else {
                    System.out.println("Unknown client type");
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                System.out.println("ERROR: " + e.getMessage());
            }
        }

        /**
         * Obtains the sleep timer and number of fitting rooms from UAClient. Initializes the sleepTimer,
         * numberOfFittingRooms, numberOfWaitingRooms, and numberOfCustomers.
         * @param in
         * @param out
         * @throws RuntimeException
         * @return String Array
         */
        private void handleUAClientCommunication(ObjectInputStream in, ObjectOutputStream out) {
            // Register UAClient
            clientOutputStreams.add(out);
            try {
                int clientSleepTimer = in.readInt();
                int clientNumberOfFittingRooms = in.readInt();
                sleepTimer.set(clientSleepTimer);
                numberOfFittingRooms.set(clientNumberOfFittingRooms);
                numberOfWaitingRooms.set(clientNumberOfFittingRooms * 2);
                // Read and process objects from the UAClient
                numberOfCustomers = numberOfFittingRooms.get() + numberOfWaitingRooms.get();

                // Respond to the UAClient
                Object serverResponse = "Server response to UAClient";
                out.writeObject(serverResponse);
                out.flush();
                initializeFittingRooms();
                customersLatch.countDown();
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }

        /**
         * Sends a message to FittingRoomServer saying the numberOfFittingRooms and numberOfWaitingRooms.
         *
         * @throws IOException
         * @return void
         */
        private static void initializeFittingRooms() {
            try {
                ObjectOutputStream fitOut = fittingRoomServerOutputStreams.get(0);
                Object messageToFittingRoom = "FITTING_ROOM#" + numberOfFittingRooms.get() + "#WAITING_ROOM#" + numberOfWaitingRooms.get();
                fitOut.writeObject(messageToFittingRoom);
                fitOut.flush();
                System.out.println("Sent message to FittingRoomServer: " + messageToFittingRoom);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("ERROR: " + e.getMessage());
            }

        }

        /**
         * Starts communicating with FittingRoomServer.
         * @param in
         * @param out
         * @throws IOException
         * @return void
         */
        private void handleFittingRoomServerCommunication(ObjectInputStream in, ObjectOutputStream out) {
            // Communicate with FittingRoomServer
            try {
                fittingRoomServerOutputStreams.add(out);
                fittingRoomServerInputStreams.add(in);
                // For example, send a message to FittingRoomServer
                Object messageToFittingRoom = "Hello from UACentralServer to FittingRoomServer";
                out.writeObject(messageToFittingRoom);
                out.flush();
                System.out.println("Sent message to FittingRoomServer: " + messageToFittingRoom);
                Object x = in.readObject();
                System.out.println("Received message from FittingRoomServer: " + x);
            } catch (Exception e) {
                System.out.println("Failed to send message to UAClient: " + e.getMessage());
            }
        }

        /**
         * Relays message to all UAClients.
         * @param message
         * @throws IOException
         * @return void
         */
        private void relayMessageToAllUAClients(Object message) {
            for (ObjectOutputStream clientOut : clientOutputStreams) {
                try {
                    clientOut.writeObject(message);
                    clientOut.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("ERROR: " + e.getMessage());
                }
            }
        }

        /**
         * Relays message to all fittingRoomServers.
         * @param message
         * @throws IOException
         * @return void
         */
        private void relayMessageToAllFittingRoomServers(Object message) {
            for (ObjectOutputStream fitOut : fittingRoomServerOutputStreams) {
                try {
                    fitOut.writeObject(message);
                    fitOut.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("ERROR: " + e.getMessage());
                }
            }
        }

        /**
         * Relays message to all UAClients.
         * @param message
         * @param out
         * @throws IOException
         * @return void
         */
        private void relayMessageToUAClient(Object message, ObjectOutputStream out) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("ERROR: " + e.getMessage());
            }

        }

        /**
         * Relays message to all FittingRoomServers.
         * @param message
         * @param out
         * @throws IOException
         * @return void
         */
        private void relayMessageToFittingRoomServer(Object message, ObjectOutputStream out) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }
}