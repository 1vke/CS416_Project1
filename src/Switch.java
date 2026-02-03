import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Switch {
    private String switchId;
    private String myIp;
    private int myPort;
    private Map<String, PortInfo> virtualPorts;
    private Map<String, PortInfo> switchTable;
    private NetworkLayer networkLayer;

    public Switch(String switchId) {
        this.switchId = switchId;
        this.virtualPorts = new HashMap<>();
        this.switchTable = new HashMap<>();
    }

    private void loadConfig(Config config){
        this.myIp = config.getIp(switchId);
        this.myPort = config.getPort(switchId);

        List<String> neighbors = config.getNeighbors(switchId);
        for (String neighborId : neighbors) {
            String neighborIp = config.getIp(neighborId);
            int neighborPort = config.getPort(neighborId);
            virtualPorts.put(neighborId, new PortInfo(neighborIp, neighborPort));
        }
    }

    public void initialize(String configFile) throws IOException {
        Config config = new Config(configFile);
        loadConfig(config);

        this.networkLayer = new NetworkLayer(myPort);

        System.out.println("Switch " + switchId + " initialized on " + myIp + ":" + myPort);
        System.out.println("Virtual ports created for neighbors: " + virtualPorts.keySet());
    }

    public void start() {
        System.out.println("Switch " + switchId + " is running");

        while (true) {
            try {
                NetworkLayer.Data data = networkLayer.receive();
                handleFrame(data);
            } catch (IOException e) {
                System.err.println("Error receiving frame: " + e.getMessage());
            }
        }
    }

    private void handleFrame(NetworkLayer.Data data) {
        String frame = data.frame();
        String senderIp = data.srcIp();
        int senderPort = data.srcPort();
        String[] parts = frame.split(":", 3);
        if (parts.length < 3) {
            System.err.println("Invalid frame format: " + frame);
            return;
        }
        String srcMAC = parts[0];
        String destMAC = parts[1];

        System.out.println(switchId + " received frame: " + frame +
                " from address " + senderIp + ":" + senderPort);

        PortInfo incomingPort = new PortInfo(senderIp, senderPort);
        boolean isNewEntry = !switchTable.containsKey(srcMAC);
        switchTable.put(srcMAC, incomingPort);

        if (isNewEntry) {
            System.out.println(switchId + " learned new MAC address: " + srcMAC);
            printSwitchTable();
        }

        if (switchTable.containsKey(destMAC)) {
            PortInfo destPort = switchTable.get(destMAC);
            System.out.println(switchId + " forwarding to known destination: " + destMAC);
            forwardFrame(frame, destPort);
        } else {
            System.out.println(switchId + " destination " + destMAC +
                    " unknown, flooding to all ports");
            flood(frame, incomingPort);
        }
    }

    private void forwardFrame(String frame, PortInfo port) {
        try {
            networkLayer.send(frame, port.ip, port.port);
            System.out.println(switchId + " sent frame to " + port.ip + ":" + port.port);
        } catch (IOException e) {
            System.err.println("Error forwarding frame to " + port.ip + ":" + port.port +
                    " - " + e.getMessage());
        }
    }

    private void flood(String frame, PortInfo incomingPort) {
        for (PortInfo port : virtualPorts.values()) {
            if (!port.equals(incomingPort)) {
                forwardFrame(frame, port);
            }
        }
    }

    private void printSwitchTable() {
        System.out.println("=================================");
        System.out.println("Switch Table for " + switchId + ":");
        System.out.println("MAC Address  ->  IP:Port");
        System.out.println("---------------------------------");
        for (Map.Entry<String, PortInfo> entry : switchTable.entrySet()) {
            System.out.println(entry.getKey() + "  ->  " +
                    entry.getValue().ip + ":" + entry.getValue().port);
        }
        System.out.println("=================================\n");
    }

    private static class PortInfo {
        String ip;
        int port;

        PortInfo(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            PortInfo portInfo = (PortInfo) obj;
            return port == portInfo.port && ip.equals(portInfo.ip);
        }

        @Override
        public int hashCode() {
            return ip.hashCode() * 31 + port;
        }
    }

    public static void main(String[] args) {
        String switchId = args[0];
        Switch sw = new Switch(switchId);
        try {
            sw.initialize("config.json");
            sw.start();
        } catch (IOException e) {
            System.err.println("Failed to initialize switch: " + e.getMessage());
        }

    }
}
