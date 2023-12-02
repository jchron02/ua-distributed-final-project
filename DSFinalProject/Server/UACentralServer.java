import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class UACentralServer {

    public static int sleepTimer;
    public static int fittingRooms;

    public static void main(String[] args) {
        int portNumber = 479;

        try {

            ServerSocket serverSocket = new ServerSocket(portNumber);

            System.out.println("UAServer is running and listening on port " + portNumber +"...");

            while (true) {

                Socket socket = serverSocket.accept();

                //--> We create an object to handle the input
                HandleInput handleInput = new HandleInput(serverSocket);

                //--> We start the run() method to see if input is from a server or the client
                handleInput.run();

                //System.out.println("A connection was received, IP address: " + socket.getInetAddress().getHostAddress());
            }
        } catch (IOException e) {

            System.out.println("Error: " + e.getMessage());
        }
    }
}

//--> Old class name was HandleClientOutput
class HandleInput implements Runnable {

    private Socket socket;
    private Logger logger;

    private String clientIP = "";
    private String frServerIP = "";

    public HandleInput(Socket socket) {

        this.socket = socket;
        configureLogger();
    }

    private void configureLogger() {

        this.logger = Logger.getLogger(HandleClientInput.class.getName());

        try {

            FileHandler fileHandler = new FileHandler("UAServer.log", true);

            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {

            System.err.println("Error setting up logger: " + e.getMessage());
        }
    }

    @Override
    public void run() {

        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            logInfo("New connection from IP address " + socket.getInetAddress().getHostAddress());

            String inputLine;

            while ((inputLine = in.readLine()) != null) {

                System.out.println("Received message from client: " + inputLine);
                logInfo("Received message from client: " + inputLine);

                //--> Checks if the connection source is a server, client or neither.
                if(inputLine.contains("server")) {

                    frServerIP = socket.getInetAddress().getHostAddress();

                    continue;
                    //fittingRoomTask();
                }
                else if(inputLine.contains("client")) {

                    clientIP = socket.getInetAddress().getHostAddress();

                    //clientTask();
                }

                //--> Close socket if source is unidentified, do I need this try catch? Could be an error here!!!
                else {

                    System.out.println("Unable to identify connection source, disconnecting...");

                    try {

                        socket.close();
                    }
                    catch(IOException e) {

                        System.err.println("Error closing client socket: " + e.getMessage());
                        logWarning("Error closing client socket: " + e.getMessage());
                    }
                }


                // Start fittingroom task.
            }

            //--> Disconnection success message
            logInfo("Client " + socket.getInetAddress().getHostAddress() + " disconnected");

        } catch (IOException e) {  //Why IOException in 2 places?

            System.err.println("Error: " + e.getMessage());
            logWarning("Error : " + e.getMessage() + " received from response from client " + socket.getInetAddress().getHostAddress());
        } finally {
            try {

                socket.close();
            } catch (IOException e) {

                System.err.println("Error closing client socket: " + e.getMessage());
                logWarning("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void fittingRoomTask() {


    }

    private void clientTask() {


    }

    private void logInfo(String message) {

        logger.log(Level.INFO, "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] INFO: " + message);
    }

    private void logWarning(String message) {
        logger.log(Level.WARNING, "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] WARNING: " + message);
    }
}