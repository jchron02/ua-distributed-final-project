/********************************
 Name: Joshua C, David T, Christopher M
 Team: 3
 Final Project
 Due Date: 
 ********************************/

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.concurrent.*;

public class FittingRoomServer {
    private final int SERVER_PORT = 576;
    private Semaphore FittingRoomLock;
    private Semaphore WaitingRoomLock;
    private int NumFittingRooms;
    private int NumWaitingChairs;
    private boolean AllowCustomerEntry;
    private static final FittingRoomServer FITTING_ROOM = new FittingRoomServer();

    public boolean allowCustomerEntry() {
        return AllowCustomerEntry;
    }

    public int getServerPort() {
        return SERVER_PORT;
    }

    public void enter(int customerId, ObjectOutputStream out) throws InterruptedException {
        if (WaitingRoomLock.tryAcquire()) {
            System.out.println("\tCustomer #" + customerId +
                    " enters the waiting area and has a seat. We have " + (NumWaitingChairs - WaitingRoomLock.availablePermits()) + " waiting");
            FittingRoomLock.acquire();
            WaitingRoomLock.release();
            System.out.println("\t\tCustomer #" + customerId +
                    " enters fitting room. We have " + (NumFittingRooms - FittingRoomLock.availablePermits()) + " changing and " +
                    (NumWaitingChairs - WaitingRoomLock.availablePermits()) + " waiting");
            // Random time spent in the fitting room :)
            Thread.sleep(new Random().nextInt(1000, 10000));
            FittingRoomLock.release();

            System.out.println("\t\t\tCustomer #" + customerId + " leaves fitting room");
        } else {
            System.out.println("\t\t\tCustomer #" + customerId +
                    " leaves due to no available chairs.");
        }
    }

    public static void main(String[] args) {
        try (ServerSocket socket = new ServerSocket(FITTING_ROOM.getServerPort())) {
            System.out.println("FittingRoom is running and listening on port " + FITTING_ROOM.getServerPort() + "...");

            while (true) {
                Socket listen = socket.accept();
                // LOG THIS AS WELL.
                System.out.println("Connection Receive from: " + listen.getInetAddress().getHostAddress());
                FITTING_ROOM.handleRequest(listen);
            }
        } catch (Exception e) {
            // Add specific logging without Exception... IOException - FileNotFoundException etc...
            // Log
        }
    }

    public void handleCustomer(int customerId, ObjectOutputStream out) {
        try {
            new Thread(new Customer(FITTING_ROOM, customerId, out)).start();
        } catch (Exception e) {
            // Add specific logging without Exception... IOException - FileNotFoundException etc...
            // Log
        }
    }

    public void handleRequest(Socket socket) {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            String centralServerMessage;
            while ((centralServerMessage = (String) in.readObject())!=null) {
                //Get Details from UACentralServer... LOG THIS AS WELL.
                if (centralServerMessage.contains("new customer")) {
                    String customerId = (String) in.readObject();
                    int id = Integer.parseInt(customerId);
                    handleCustomer(id, out);
                } else if (centralServerMessage.contains("fitting room permits")) {
                    out.writeObject(FITTING_ROOM.FittingRoomLock.availablePermits());
                } else if (centralServerMessage.contains("waiting room permits")) {
                    out.writeObject(FITTING_ROOM.WaitingRoomLock.availablePermits());
                }
                // Process message from UACentralServer and return to UACentralServer and UAClient... LOG THIS AS WELL.
                String responseMessage = "Response from FittingRoomServer.";
                sendResponse(out, responseMessage);
            }
        } catch (Exception e) {
            // Add specific logging without Exception... IOException - FileNotFoundException etc...
            // Log
        }
    }

    public void sendResponse(ObjectOutputStream out, String responseMessage) {
        try {
            // Send the response message to UACentralServer
            out.writeObject(responseMessage);
            out.flush();
            System.out.println("Sent message to UACentralServer: " + responseMessage);
        } catch (IOException e) {
            // Handle the exception. LOG THIS AS WELL.
            e.printStackTrace();
        }
    }
}

class Customer implements Runnable {
    private FittingRoomServer fittingRoom;
    private int CustomerID;
    private ObjectOutputStream Out;

    public Customer(FittingRoomServer fittingRoom, int customerId, ObjectOutputStream out) {
        this.fittingRoom = fittingRoom;
        this.CustomerID = customerId;
        Out = out;
    }

    @Override
    public void run() {
        try {
            if (fittingRoom.allowCustomerEntry()) {
                System.out.println("Customer #" + this.CustomerID + " enters the system");
                //Log this.
                fittingRoom.enter(this.CustomerID, this.Out);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}