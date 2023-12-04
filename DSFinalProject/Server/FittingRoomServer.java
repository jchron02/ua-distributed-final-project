/********************************
 Name: Joshua C, David T, Christopher M
 Team: 3
 Final Project
 Due Date: 
 ********************************/

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class FittingRoomServer {
    private final int SERVER_PORT = 576;
    private Semaphore FittingRoomLock = new Semaphore(5);
    private Semaphore WaitingRoomLock = new Semaphore(10);

    public int getServerPort() {
        return SERVER_PORT;
    }

    public static void main(String[] args) {
        FittingRoomServer fr = new FittingRoomServer();
        try (ServerSocket socket = new ServerSocket(fr.getServerPort())) {
            System.out.println("FittingRoom is running and listening on port " + fr.getServerPort() + "...");

            while (true) {
                Socket clientSocket = socket.accept();
                // LOG THIS AS WELL.
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                fr.handleClient(clientSocket);
            }
        } catch (Exception e) {
            // Add specific logging without Exception... IOException - FileNotFoundException etc...
            // Log
        }
    }

    public void handleClient(Socket clientSocket) {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

            //Get Details from UACentralServer... LOG THIS AS WELL.
            String centralServerMessage = (String) in.readObject();
            System.out.println("Received message from UACentralServer: " + centralServerMessage);

            // Process message from UACentralServer and return to UACentralServer and UAClient... LOG THIS AS WELL.
            String responseMessage = "Response from FittingRoomServer.";
            out.writeObject(responseMessage);
            System.out.println("Sent message to UACentralServer: " + responseMessage);
        } catch (Exception e) {
            // Add specific logging without Exception... IOException - FileNotFoundException etc...
            // Log
        }
    }
}