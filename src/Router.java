import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Router {
    private final String routerId;
    private String myIp;
    private int myPort;
    private final Map<String, PortInfo> virtualPorts;
    private final Map<String, RoutingEntry> routingTable;
    private NetworkLayer networkLayer;

    public Router(String routerId) {
        this.routerId = routerId;
        this.virtualPorts = new HashMap<>();
        this.routingTable = new HashMap<>();
    }

    private void loadConfig(Config config) {
        this.myIp = config.getIp(routerId);
        this.myPort = config.getPort(routerId);

        List<String> neighbors = config.getNeighbors(routerId);
        for (String neighborId : neighbors) {
            String neighborIp = config.getIp(neighborId);
            int neighborPort = config.getPort(neighborId);
            String portName = neighborIp + ":" + neighborPort;
            virtualPorts.put(portName, new PortInfo(neighborIp, neighborPort, neighborId));
        }

        loadRoutingTable(config);
    }

    private void loadRoutingTable(Config config) {
        List<Config.RoutingTableEntry> entries = config.getRoutingTable(routerId);
        if (entries != null) {
            for (Config.RoutingTableEntry entry : entries) {
                routingTable.put(entry.subnet, new RoutingEntry(entry.subnet, entry.nextHop));
            }
        }

        System.out.println("\n+-------------------------------------------+");
        System.out.println("| Routing Table for " + String.format("%-23s", routerId) + "|");
        System.out.println("+----------------------+--------------------+");
        System.out.println("| Subnet Prefix        | Next-hop/Exit Port |");
        System.out.println("+----------------------+--------------------+");
        for (Map.Entry<String, RoutingEntry> entry : routingTable.entrySet()) {
            System.out.printf("| %-20s | %-18s |%n",
                    entry.getKey(),
                    entry.getValue().nextHopOrPort);
        }
        System.out.println("+----------------------+--------------------+\n");
    }

    public void initialize(String configFile) throws IOException {
        Config config = new Config(configFile);
        loadConfig(config);

        this.networkLayer = new NetworkLayer(myPort);

        System.out.println("Router " + routerId + " initialized on " + myIp + ":" + myPort);
        System.out.println("Virtual ports created for neighbors: " + virtualPorts.keySet());
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void start() {
        System.out.println("Router " + routerId + " is running");

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

        Packet packet = Packet.parse(frame);
        if (packet == null) {
            System.err.println("Invalid packet format: " + frame);
            return;
        }

        System.out.println("\n[" + routerId + "] RECEIVED Packet:");
        System.out.println("  Type: " + (packet.getType() == Packet.TYPE_USER ? "USER" : "ROUTING"));
        System.out.println("  Virtual Source MAC: " + packet.getSrcMAC());
        System.out.println("  Virtual Dest MAC: " + packet.getDestMAC());
        System.out.println("  Virtual Source IP: " + packet.getSrcIP());
        System.out.println("  Virtual Dest IP: " + packet.getDestIP());
        System.out.println("  Payload: " + packet.getPayload());
        System.out.println("  From: " + senderIp + ":" + senderPort);

        if (!packet.getDestMAC().equals(routerId)) {
            System.out.println("[" + routerId + "] Packet not for me (dest MAC: " + packet.getDestMAC() + "), dropping.");
            return;
        }

        if (packet.getType() == Packet.TYPE_ROUTING) {
            processRoutingUpdate(packet);
            return;
        }

        String destSubnet = extractSubnet(packet.getDestIP());

        RoutingEntry routingEntry = routingTable.get(destSubnet);
        if (routingEntry == null) {
            System.err.println("[" + routerId + "] No route to subnet: " + destSubnet);
            return;
        }

        System.out.println("[" + routerId + "] Routing decision: " + destSubnet + " -> " + routingEntry.nextHopOrPort);

        String newDestMAC;
        PortInfo outgoingPort;

        if (routingEntry.nextHopOrPort.contains(".")) {
            newDestMAC = extractHostId(routingEntry.nextHopOrPort);

            outgoingPort = findPortByNeighborId(newDestMAC);

            if (outgoingPort == null) {
                System.err.println("[" + routerId + "] Cannot find port for next-hop router: " + newDestMAC);
                return;
            }
        } else {
            newDestMAC = extractHostId(packet.getDestIP());

            outgoingPort = findPortByNeighborId(routingEntry.nextHopOrPort);

            if (outgoingPort == null) {
                System.err.println("[" + routerId + "] Cannot find outgoing port for neighbor: " + routingEntry.nextHopOrPort);
                return;
            }
        }

        packet.setSrcMAC(routerId);
        packet.setDestMAC(newDestMAC);

        System.out.println("\n[" + routerId + "] FORWARDING Packet:");
        System.out.println("  Type: " + (packet.getType() == Packet.TYPE_USER ? "USER" : "ROUTING"));
        System.out.println("  Virtual Source MAC: " + packet.getSrcMAC());
        System.out.println("  Virtual Dest MAC: " + packet.getDestMAC());
        System.out.println("  Virtual Source IP: " + packet.getSrcIP());
        System.out.println("  Virtual Dest IP: " + packet.getDestIP());
        System.out.println("  Payload: " + packet.getPayload());
        System.out.println("  To: " + outgoingPort.ip + ":" + outgoingPort.port);

        forwardFrame(packet.toString(), outgoingPort);
    }

    private void processRoutingUpdate(Packet packet) {
        System.out.println("[" + routerId + "] Processing routing update from " + packet.getSrcIP());
        // TODO: Implement dynamic routing logic (DV or LS) here
    }

    private String extractSubnet(String virtualIP) {
        int dotIndex = virtualIP.indexOf('.');
        if (dotIndex > 0) {
            return virtualIP.substring(0, dotIndex);
        }
        return virtualIP;
    }

    private String extractHostId(String virtualIP) {
        int dotIndex = virtualIP.indexOf('.');
        if (dotIndex > 0 && dotIndex < virtualIP.length() - 1) {
            return virtualIP.substring(dotIndex + 1);
        }
        return virtualIP;
    }

    private PortInfo findPortByNeighborId(String neighborId) {
        for (PortInfo port : virtualPorts.values()) {
            if (port.neighborId != null && port.neighborId.equals(neighborId)) {
                return port;
            }
        }
        return null;
    }

    private void forwardFrame(String frame, PortInfo port) {
        try {
            networkLayer.send(frame, port.ip, port.port);
            System.out.println("[" + routerId + "] Successfully transmitted frame to " + port.ip + ":" + port.port);
        } catch (IOException e) {
            System.err.println("Error forwarding frame to " + port.ip + ":" + port.port +
                    " - " + e.getMessage());
        }
    }

    private static class PortInfo {
        String ip;
        int port;
        String neighborId;

        PortInfo(String ip, int port, String neighborId) {
            this.ip = ip;
            this.port = port;
            this.neighborId = neighborId;
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

    private static class RoutingEntry {
        String subnet;
        String nextHopOrPort;

        RoutingEntry(String subnet, String nextHopOrPort) {
            this.subnet = subnet;
            this.nextHopOrPort = nextHopOrPort;
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Router <routerID> [configFile]");
            return;
        }

        String routerId = args[0];
        String configFile = (args.length > 1) ? args[1] : "resources/config.json";

        Router router = new Router(routerId);
        try {
            router.initialize(configFile);
            router.start();
        } catch (IOException e) {
            if (args.length > 1) {
                System.out.println("Failed to load config from " + configFile + ", trying default resource...");
                try {
                    router.initialize("resources/config.json");
                    router.start();
                } catch (IOException ex) {
                    System.err.println("Failed to initialize router with default config: " + ex.getMessage());
                }
            } else {
                System.err.println("Failed to initialize router: " + e.getMessage());
            }
        }
    }
}