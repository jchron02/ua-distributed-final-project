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
    private static int waitingRoomIndex = 0;
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
                    new Thread(UACentralServer::communicateWithFittingRoomServers).start();
                }
            }).start();


            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connected to a client");

                // Handle the client connection in a separate thread
                ConnectionHandler connectionHandler = new ConnectionHandler(clientSocket);
                new Thread(connectionHandler).start();


            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String[] getPermitValues(String permitsMessage) {
        // Parse the permits message and update permits
        String[] parts = permitsMessage.split("#");
        if (parts.length == 3 && "PermitsInfo".equals(parts[0])) {
            return parts;
        } else {
            throw new RuntimeException("Invalid permits message format");
        }

    }

    private static void createNewCustomer(String type) {
        if (type.equals("FITTING_ROOM")) {
            ObjectOutputStream fittingRoomServerOut = fittingRoomServerOutputStreams.get(fittingRoomIndex);
            handleCustomerRequest(fittingRoomServerOut);
        } else if (type.equals("WAITING")) {
            ObjectOutputStream waitingRoomServerOut = fittingRoomServerOutputStreams.get(waitingRoomIndex);
            handleCustomerRequest(waitingRoomServerOut);
        }
    }

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

    private static void communicateWithFittingRoomServers() {
        // Send a message to all FittingRoomServers with permits information
        int fittingRoomPermitsCheck = 0;
        int waitingChairPermitsCheck = 0;
        int greatestAmountFRS = 0;
        int greatestAmountWCRS = 0;
        Object permitsMessage = "REQUESTING_PERMITS";
        for (int i = 0; i < fittingRoomServerOutputStreams.size(); i++) {
            try {
                ObjectOutputStream fittingRoomServerOut = fittingRoomServerOutputStreams.get(i);
                ObjectInputStream fittingRoomServerIn = fittingRoomServerInputStreams.get(i);
                fittingRoomServerOut.writeObject(permitsMessage);
                fittingRoomServerOut.flush();

                Object responseMessage = fittingRoomServerIn.readObject();
                String[] parts = getPermitValues((String) responseMessage);
                int numFittingRooms = Integer.parseInt(parts[1]);
                int numWaitingChairs = Integer.parseInt(parts[2]);
                fittingRoomPermitsCheck += numFittingRooms;
                waitingChairPermitsCheck += numWaitingChairs;

                if (numFittingRooms > greatestAmountFRS) {
                    greatestAmountFRS = i + 1;
                }
                if (numWaitingChairs > greatestAmountWCRS) {
                    greatestAmountWCRS = i + 1;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            fittingRoomPermits.set(fittingRoomPermitsCheck);
            waitingChairPermits.set(waitingChairPermitsCheck);
        }

        if (greatestAmountFRS > 0) {
            fittingRoomIndex = greatestAmountFRS - 1;
            createNewCustomer("FITTING");
        } else if (greatestAmountWCRS > 0) {
            waitingRoomIndex = greatestAmountWCRS - 1;
            createNewCustomer("WAITING");
        } else {
            // Handle the case when no fitting rooms are available
            System.out.println("No fitting rooms available");
        }
    }


    private static class ConnectionHandler implements Runnable {
        private final Socket connectionSocket;

        public ConnectionHandler(Socket clientSocket) {
            this.connectionSocket = clientSocket;
        }

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
            }
        }

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
                Object clientInput = in.readObject();
                System.out.println("Received from UAClient: " + clientInput);

                numberOfCustomers = numberOfFittingRooms.get() + numberOfWaitingRooms.get();
                customersLatch.countDown();
                // Respond to the UAClient
                Object serverResponse = "Server response to UAClient";
                out.writeObject(serverResponse);

                out.flush();
                System.out.println("Sent response to UAClient");
            } catch (Exception ex) {

            }
        }

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

                // Receive a message from FittingRoomServer
                Object fittingRoomResponse = in.readObject();
                System.out.println("Received from FittingRoomServer: " + fittingRoomResponse);

                // Relay the message to all connected UAClients
                relayMessageToAllUAClients(fittingRoomResponse);
            } catch (Exception ex){

            }
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
            for (ObjectOutputStream fitOut : fittingRoomServerOutputStreams) {
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