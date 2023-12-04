import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

/********************************
 Name: Joshua C, David T, Christopher M
 Team: 3
 Final Project
 Due Date: 
 ********************************/
public class UAClient {

    public static void main(String[] args) {
        String serverHostname = "localhost";
        int serverPortNumber = 576;

        try (Socket socket = new Socket(serverHostname, serverPortNumber)){
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            System.out.println("Welcome to the UAClient Terminal Interface!\n" +
                    "Please enter your command or type ’exit’ to quit.\n \n" +
                    "Available Commands:\n" +
                    "- IP: Retrieve client’s IP Address\n" +
                    "- TIME: Get the current time\n" +
                    "- MATH: Perform basic multiplication\n");
            System.out.print("Enter command: ");

            String inputLine;
            while ((inputLine = (String) in.readObject()) != null) {
                if ("exit".equalsIgnoreCase(inputLine)) {
                    break;
                }

                out.writeObject(inputLine);
                out.flush();
                String serverResponse = (String) in.readObject();
                System.out.print("Server Response: " + serverResponse + "\n");
            }
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + serverHostname);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}