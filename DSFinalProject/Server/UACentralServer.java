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

            while (true) { //Does the client just send 3 messages then waits on feedback?

                Socket socket = serverSocket.accept();

                //--> We create an object to handle the input
                HandleInput handleInput = new HandleInput(socket);

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
        //configureLogger();
    }

    /*private void configureLogger() {

        this.logger = Logger.getLogger(HandleClientInput.class.getName());

        try {

            FileHandler fileHandler = new FileHandler("UAServer.log", true);

            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {

            System.err.println("Error setting up logger: " + e.getMessage());
        }
    }*/

    @Override
    public void run() {

        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            //-> This decides what is done to the message in the task methods
            int task = 0;

            //-> Turns true if we have a server connection
            boolean isServer = false;

            logInfo("New connection from IP address " + socket.getInetAddress().getHostAddress());

            String inputLine;

            while ((inputLine = in.readLine()) != null) {

                System.out.println("Received message from client: " + inputLine);
                logInfo("Received message from client: " + inputLine);

                //--> Checks if the connection source is a server, client or neither.
                if(inputLine.contains("server") || isServer) {

                    fittingRoomTask(inputLine, task);

                    isServer = true;

                    task++;
                }
                else if(inputLine.contains("client")) {

                    clientTask(inputLine, task);

                    task++;
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

    private void fittingRoomTask(String inputLine, int i) {

        switch (i) {

            case 0:

                this.frServerIP = socket.getInetAddress().getHostAddress();

                break;
            case 1:


        }
    }

    private void clientTask(String inputLine, int i) {

        switch(i) {

            case 0:

                //-> Retrieve IP to put in the clientIP global var
                this.clientIP = socket.getInetAddress().getHostAddress();

                break;
            case 1:

                //-> Splitting the string, we need two numbers, sleep timer and fitting rooms respectively
                String[] elements;

                int j = 0;

                elements = inputLine.split("[\\s.,?!;:]");

                for(String element : elements) {

                    if(element.contains("->") & j == 0) {

                        UACentralServer.sleepTimer = Integer.parseInt(element.substring(2));

                        j++;
                    }
                    else if(element.contains("->") & j == 1) {

                        UACentralServer.fittingRooms = Integer.parseInt(element.substring(2));

                        break;
                        //j++;
                    }
                }

                break;
                //We need to make sure that we have the sleep timer and fitting rooms before this point
        }
    }

    private void logInfo(String message) {

        logger.log(Level.INFO, "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] INFO: " + message);
    }

    private void logWarning(String message) {
        logger.log(Level.WARNING, "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] WARNING: " + message);
    }
}