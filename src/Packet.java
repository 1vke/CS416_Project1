public class Packet {
    public static final int TYPE_USER = 0;
    public static final int TYPE_ROUTING = 1;

    private final int type;
    private String srcMAC;
    private String destMAC;
    private final String srcIP;
    private final String destIP;
    private final String payload;

    public Packet(int type, String srcMAC, String destMAC, String srcIP, String destIP, String payload) {
        this.type = type;
        this.srcMAC = srcMAC;
        this.destMAC = destMAC;
        this.srcIP = srcIP;
        this.destIP = destIP;
        this.payload = payload;
    }

    public static Packet parse(String frame) {
        if (frame == null) return null;
        String[] parts = frame.split(":", 6);
        if (parts.length < 6) {
            // Here we could support legacy format for transition if necessary (prob not), but here we enforce the new format
            // Old format was srcMAC:destMAC:srcIP:destIP:message
            return null;
        }

        try {
            int type = Integer.parseInt(parts[0]);
            return new Packet(type, parts[1], parts[2], parts[3], parts[4], parts[5]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return type + ":" + srcMAC + ":" + destMAC + ":" + srcIP + ":" + destIP + ":" + payload;
    }

    public int getType() { return type; }
    public String getSrcMAC() { return srcMAC; }
    public String getDestMAC() { return destMAC; }
    public String getSrcIP() { return srcIP; }
    public String getDestIP() { return destIP; }
    public String getPayload() { return payload; }

    public void setSrcMAC(String srcMAC) { this.srcMAC = srcMAC; }
    public void setDestMAC(String destMAC) { this.destMAC = destMAC; }
}
