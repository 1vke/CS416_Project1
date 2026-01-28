import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkLayer {
    private final DatagramSocket socket;

    /**
     * Inner class to hold received data AND the physical source (needed for Switch learning)
     * @param frame   The frame content (e.g., "A:B:hello")
     * @param srcIp   The physical sender's IP
     * @param srcPort The physical sender's Port
     */
    public record Data(String frame, String srcIp, int srcPort) {}

    public NetworkLayer(int port) throws SocketException {
        this.socket = new DatagramSocket(port);
    }

    public void send(String message, String destIp, int destPort) throws IOException {
        byte[] buffer = message.getBytes();
        InetAddress address = InetAddress.getByName(destIp);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, destPort);

        socket.send(packet);
    }

    public Data receive() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        socket.receive(packet);

        String msg = new String(packet.getData(), 0, packet.getLength());
        String senderIp = packet.getAddress().getHostAddress();
        int senderPort = packet.getPort();

        return new Data(msg, senderIp, senderPort);
    }

    public void close() {
        if (!socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * Example of how to use this class
     */
    public static void main(String[] args) {
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            int receiverPort = 9001;
            int senderPort = 9002;
            String message = "Hello from NetworkLayer Main with Executors!";

            // Receiver Task
            executor.submit(() -> {
                System.out.println("[Receiver] Starting on port " + receiverPort);
                try {
                    NetworkLayer receiver = new NetworkLayer(receiverPort);
                    Data data = receiver.receive();
                    System.out.println("[Receiver] Received: " + data.payload() +
                            " from " + data.srcIp() + ":" + data.srcPort());
                    receiver.close();
                } catch (Exception e) {
                    System.err.println("[Receiver] Error: " + e.getMessage());
                }
            });

            // Give receiver a moment to bind
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Sender Task
            executor.submit(() -> {
                System.out.println("[Sender] Starting on port " + senderPort);
                try {
                    NetworkLayer sender = new NetworkLayer(senderPort);
                    sender.send(message, "127.0.0.1", receiverPort);
                    System.out.println("[Sender] Sent: " + message);
                    sender.close();
                } catch (Exception e) {
                    System.err.println("[Sender] Error: " + e.getMessage());
                }
            });

            executor.shutdown();
            try {
                if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    System.err.println("Test timed out, forcing shutdown.");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
        System.out.println("[Main] Test finished.");
    }
}
