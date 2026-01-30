import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Host {

    private String hostID;
    private String mac;
    private String myIp;
    private int myPort;
    private String switchIP;
    private int switchPort;

    private NetworkLayer networkLayer;

    private Host(String hostID) {
        this.mac = hostID;
        this.hostID = hostID;

    }

    private void initalize(String configFile) throws IOException {
        //load config to find ip, port, and neighbors
        networkLayer = new NetworkLayer(myPort);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(this::sender);
        executor.submit(this::receiver);

        System.out.println("Host " + hostID + " initialized on " +
                myIp + ":" + myPort);

    }

    //sender
    private void sender() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Destination MAC: ");
            String destMac = scanner.nextLine();

            System.out.print("Message: ");
            String message = scanner.nextLine();

            String frame = mac + ":" + destMac + ":" + message;

            try {
                networkLayer.send(frame, switchIP, switchPort);
            } catch (IOException e) {
                System.out.println("[Host " + hostID + "] Failed to send frame");
            }
        }
    }

    //receiver
    private void receiver() {
        while (true) {
            try {
                NetworkLayer.Data data = networkLayer.receive();
                String[] parts = data.frame().split(":", 3);
                if (parts.length < 3){
                    continue;
                }

                    String srcMac = parts[0];
                String destMac = parts[1];
                String message = parts[2];

                if (destMac.equals(mac)) {
                    System.out.println("Message from " + srcMac + ": " + message);
                } else {
                    System.out.println("MAC mismatch");
                }
            } catch (IOException e) {
                System.out.println("Receive error");
            }
        }
    }

    //main
    public static void main(String[] args) {
        String hostID = args[0];
         Host host = new Host(hostID);
         Config config = new Config(/*'config.json*/);
         //host.initalize(config);
    }

}
