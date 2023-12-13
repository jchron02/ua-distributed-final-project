import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

public class UAClient {
    private static final String HOST = "localhost";
    private static final int PORT = 479;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java UAClient <numFittingRooms> <systemTime>");
            return;
        }

        int numFittingRooms = Integer.parseInt(args[0]);
        long systemTime = Long.parseLong(args[1]);

        try (Socket cs = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(cs.getOutputStream(), true)) {

            // Sending information to UACentralServer
            out.println("CLIENT_INFO#" + numFittingRooms + "#" + systemTime);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
