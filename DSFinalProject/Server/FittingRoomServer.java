import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class FittingRoomServer {

    private static final String HOST = "localhost";
    private static final int PORT = 252;
    private Socket centralServerSocket;
    private BufferedReader centralServerIn;
    private PrintWriter centralServerOut;

    private int waitingCount = 0;
    private int numRooms;
    private int numSeats;
    private Semaphore seatController;
    private Semaphore roomController;
    private int serverId;

    public FittingRoomServer() {
        try {
            // Connect to CentralServer
            centralServerSocket = new Socket(HOST, PORT);
            centralServerIn = new BufferedReader(new InputStreamReader(centralServerSocket.getInputStream()));
            centralServerOut = new PrintWriter(centralServerSocket.getOutputStream(), true);

            // Register with CentralServer
            centralServerOut.println("FITTING_ROOM");
            centralServerOut.flush();
            // Get server ID and initial configuration
            serverId = Integer.parseInt(centralServerIn.readLine());
            String countMessage = centralServerIn.readLine();

            if (countMessage.startsWith("FITTING_ROOM#")) {
                String[] split = countMessage.split("#");
                numRooms = Integer.parseInt(split[1]);
                numSeats = Integer.parseInt(split[3]);
                seatController = new Semaphore(numSeats);
                roomController = new Semaphore(numRooms);
            }

            centralServerOut.println("Fitting Room Server #" + serverId + " is ready.");

            // Start accepting customers
            acceptCustomers();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void close(Socket socket) {
        try {
            socket.close();
            System.out.println("Connection Closed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptCustomers() throws IOException, InterruptedException {
        String line;
        while ((line = centralServerIn.readLine()) != null) {
            if (line.startsWith("NEW_CUSTOMER#")) {
                int customerID = Integer.parseInt(line.split("#")[1]);
                Customer customer = new Customer(customerID, this);
                customer.start();
            }
        }
    }

    public void enterFitting(int customerID) throws InterruptedException, IOException {
        roomController.acquire();
        leaveWaiting();
        System.out.println("\t\tCustomer #" + customerID + " enters the Fitting Room located at <Server " + serverId + ": " + InetAddress.getLocalHost() + ">");
        centralServerOut.println("\t\tCustomer #" + customerID + " enters the Fitting Room located at <Server " + serverId + ": " + InetAddress.getLocalHost() + ">");
        System.out.println("\t\tWe have " + seatController.availablePermits() + " waiting and " + roomController.availablePermits() + " changing");
        centralServerOut.println("\t\tWe have " + seatController.availablePermits() + " waiting and " + roomController.availablePermits() + " changing");
    }

    public void leaveFitting(int customerID) throws InterruptedException, IOException {
        roomController.release();
        System.out.println("\t\t\tCustomer #" + customerID + " leaves the  Fitting Room.");
        centralServerOut.println("\t\t\tCustomer #" + customerID + " leaves the  Fitting Room.");
    }

    public void enterWaiting(int customerID) throws InterruptedException, IOException {
        if (seatController.tryAcquire()) {
            waitingCount++;
            System.out.println("\tCustomer #" + customerID + " enters the waiting area on <Server " + serverId + ": " + InetAddress.getLocalHost() + "> and has a seat.");
            centralServerOut.println("\tCustomer #" + customerID + " enters the waiting area on <Server " + serverId + ": " + InetAddress.getLocalHost() + "> and has a seat.");

            System.out.println("\tWe have " + waitingCount + " waiting on <Server " + serverId + ": " + InetAddress.getLocalHost());
            centralServerOut.println("\tWe have " + waitingCount + " waiting on <Server " + serverId + ": " + InetAddress.getLocalHost());
        } else {
            System.out.println("\tCustomer #" + customerID + " could not find a seat and leaves in frustration.");
            centralServerOut.println("\tCustomer #" + customerID + " could not find a seat and leaves in frustration.");
        }
    }

    public void leaveWaiting() {
        seatController.release();
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
            System.out.println("Customer #" + customerID + " enters the system");
            try {
                fit.enterWaiting(customerID);
                fit.enterFitting(customerID);
                Thread.sleep(new Random().nextInt(1000));
                fit.leaveFitting(customerID);
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        FittingRoomServer fr = new FittingRoomServer();
    }
}
