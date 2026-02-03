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

    private void initialize(String configFile) throws IOException {
        //load config to find ip, port, and neighbors
        Config config = new Config(configFile);
        myIp = config.getIp(hostID);
        myPort = config.getPort(hostID);

        String switchId = config.getNeighbors(hostID).get(0);
        switchIP = config.getIp(switchId);
        switchPort = config.getPort(switchId);

        networkLayer = new NetworkLayer(myPort);

        System.out.println("Host " + hostID + " initialized on " +
                myIp + " : " + myPort);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(this::sender);
        executor.submit(this::receiver);
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
                System.out.println("Host " + hostID + " Failed to send frame");
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
        if (args.length != 1) {
            System.out.println("Usage: java Host <hostID>");
            return;
        }

        Host host = new Host(args[0]);
        try {
            host.initialize("config.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

}
