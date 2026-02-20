import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class Config {

    public Map<String, DeviceInfo> devices = new HashMap<>();
    public Map<String, List<String>> links = new HashMap<>();
    public Map<String, List<RoutingTableEntry>> routingTables = new HashMap<>();

    public static class DeviceInfo {
        public String id;
        public String ip;
        public int port;
        public List<String> virtualIPs = new ArrayList<>();
        public String gateway;

        public DeviceInfo(String id, String ip, int port) {
            this.id = id;
            this.ip = ip;
            this.port = port;
        }
    }

    public static class RoutingTableEntry {
        public String subnet;
        public String nextHop;

        public RoutingTableEntry(String subnet, String nextHop) {
            this.subnet = subnet;
            this.nextHop = nextHop;
        }
    }

    public Config(String filename) throws IOException {
        parse(filename);
    }

    private void parse(String filename) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        JSONObject json = new JSONObject(sb.toString());

        JSONArray devArray = json.getJSONArray("devices");
        for (int i = 0; i < devArray.length(); i++) {
            JSONObject obj = devArray.getJSONObject(i);

            String id = obj.getString("id");
            String ip = obj.getString("ip");
            int port = obj.getInt("port");

            DeviceInfo device = new DeviceInfo(id, ip, port);

            if (obj.has("virtualIPs")) {
                JSONArray vips = obj.getJSONArray("virtualIPs");
                for (int j = 0; j < vips.length(); j++) {
                    device.virtualIPs.add(vips.getString(j));
                }
            }

            if (obj.has("gateway")) {
                device.gateway = obj.getString("gateway");
            }

            devices.put(id, device);
            links.putIfAbsent(id, new ArrayList<>());
        }


        JSONArray linkArray = json.getJSONArray("links");
        for (int i = 0; i < linkArray.length(); i++) {
            JSONArray pair = linkArray.getJSONArray(i);
            String a = pair.getString(0);
            String b = pair.getString(1);

            links.get(a).add(b);
            links.get(b).add(a);
        }

        if (json.has("routingTables")) {
            JSONObject rtObj = json.getJSONObject("routingTables");
            for (String routerId : rtObj.keySet()) {
                JSONArray entries = rtObj.getJSONArray(routerId);
                List<RoutingTableEntry> list = new ArrayList<>();
                for (int i = 0; i < entries.length(); i++) {
                    JSONObject entry = entries.getJSONObject(i);
                    list.add(new RoutingTableEntry(entry.getString("subnet"), entry.getString("nextHop")));
                }
                routingTables.put(routerId, list);
            }
        }
    }

    public String getIp(String id) {
        return devices.get(id).ip;
    }

    public int getPort(String id) {
        return devices.get(id).port;
    }

    public List<String> getNeighbors(String id) {
        return links.get(id);
    }

    public String getVirtualIp(String id) {
        DeviceInfo device = devices.get(id);
        if (device.virtualIPs.isEmpty()) return null;
        return device.virtualIPs.getFirst();
    }

    public String getGateway(String id) {
        return devices.get(id).gateway;
    }

    public List<RoutingTableEntry> getRoutingTable(String routerId) {
        return routingTables.get(routerId);
    }
}