import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Host {
    private final String hostID;
    private final String mac;
    private String switchIP;
    private int switchPort;

    private String srcIP;
    private String gatewayMac;

    private NetworkLayer networkLayer;

    private Host(String hostID) {
        this.mac = hostID;
        this.hostID = hostID;
    }

    @SuppressWarnings("SameParameterValue")
    private void initialize(String configFile) throws IOException {
        //load config
        Config config = new Config(configFile);

        String myIp = config.getIp(hostID);
        int myPort = config.getPort(hostID);

        this.srcIP = config.getVirtualIp(hostID);
        String gateway = config.getGateway(hostID);
        
        if (gateway != null && gateway.contains(".")) {
            this.gatewayMac = gateway.substring(gateway.lastIndexOf('.') + 1);
        } else {
            this.gatewayMac = gateway;
        }

        String switchId = config.getNeighbors(hostID).getFirst();
        switchIP = config.getIp(switchId);
        switchPort = config.getPort(switchId);

        networkLayer = new NetworkLayer(myPort);

        System.out.println("Host " + hostID + " initialized on " +
                myIp + " : " + myPort);
        System.out.println("Virtual IP: " + srcIP);
        System.out.println("Gateway MAC: " + gatewayMac);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            executor.submit(this::sender);
            executor.submit(this::receiver);
        }
    }

    //sender
    @SuppressWarnings("InfiniteLoopStatement")
    private void sender() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Virtual destination IP: ");
            String destIP = scanner.nextLine();

            System.out.print("Message: ");
            String message = scanner.nextLine();

            String targetMac;
            if (extractSubnet(srcIP).equals(extractSubnet(destIP))) {
                targetMac = extractHostId(destIP);
            } else {
                targetMac = gatewayMac;
            }

            String frame = mac + ":" + targetMac + ":" + srcIP + ":" + destIP + ":" + message;

            try {
                networkLayer.send(frame, switchIP, switchPort);
            } catch (IOException e) {
                System.out.println("Host " + hostID + " Failed to send frame");
            }
        }
    }

    private String extractSubnet(String virtualIP) {
        int dotIndex = virtualIP.indexOf('.');
        if (dotIndex > 0) {
            return virtualIP.substring(0, dotIndex);
        }
        return virtualIP;
    }

    private String extractHostId(String virtualIP) {
        int dotIndex = virtualIP.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < virtualIP.length() - 1) {
            return virtualIP.substring(dotIndex + 1);
        }
        return virtualIP;
    }

    //receiver
    @SuppressWarnings("InfiniteLoopStatement")
    private void receiver() {
        while (true) {
            try {
                NetworkLayer.Data data = networkLayer.receive();
                String[] parts = data.frame().split(":", 5);
                if (parts.length < 5){
                    continue;
                }

                String destMac = parts[1];
                String srcIP = parts[2];
                String message = parts[4];

                if (destMac.equals(mac)) {
                    System.out.println("Message from " + srcIP + ": " + message);
                } else {
                    System.out.println("Debug: MAC address mismatch - received " + destMac + " Mac: " + mac + ". (Flooded frame)");
                }
            } catch (IOException e) {
                System.out.println("Receive error");
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Host <hostID> [configFile]");
            return;
        }

        String hostId = args[0];
        String configFile = (args.length > 1) ? args[1] : "resources/config.json";

        Host host = new Host(hostId);
        try {
            host.initialize(configFile);
        } catch (IOException e) {
            if (args.length > 1) {
                System.out.println("Failed to load config from " + configFile + ", trying default resource...");
                try {
                    host.initialize("resources/config.json");
                    return;
                } catch (IOException ex) {
                    throw new RuntimeException("Failed to load default config", ex);
                }
            }
            throw new RuntimeException(e);
        }
    }
}
