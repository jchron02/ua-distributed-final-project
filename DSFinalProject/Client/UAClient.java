import java.io.*;
import java.net.*;

public class UAClient {
    private final String HOST = "localhost";
    private final int PORT = 479;
    private Socket centralServerSocket;
    private int numFittingRooms;
    private int systemTime;
    private BufferedReader in;
    private PrintWriter out;

    /**
     * Creates a new UAClient.
     * @param systemTime
     * @param numFittingRooms
     * @throws RuntimeException
     *
     */
    private UAClient(int systemTime,int numFittingRooms) {
        this.numFittingRooms = numFittingRooms;
        this.systemTime = systemTime;
        try  {
            centralServerSocket = new Socket(HOST, PORT);
            serverListener(centralServerSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java UAClient <systemTime> <numFittingRooms>");
            return;
        }
        UAClient client = new UAClient(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
    }

    /**
     * Listens and takes in arguments from the central server.
     * @param socket
     *
     *
     */
    public void serverListener(Socket socket) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                    out.println("ARGUMENTS_" + systemTime + "_" + numFittingRooms);
                    String line;
                    while (socket.isConnected() && (line = in.readLine()) != null) {
                        if (line.startsWith("ACCOUNTED_")) {
                            out.println("DISCONNECTED_THANKS_BYE");
                            break;
                        }else{
                            System.out.println(line);
                        }
                    }
                    in.close();
                    out.close();
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }


}