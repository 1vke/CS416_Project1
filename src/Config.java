import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class Config {
    public Map<String, DeviceInfo> devices = new HashMap<>();
    public Map<String, List<String>> links = new HashMap<>();

    public static class DeviceInfo {
        public String id;
        public String ip;
        public int port;

        public DeviceInfo(String id, String ip, int port) {
            this.id = id;
            this.ip = ip;
            this.port = port;
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

        // Parse devices
        JSONArray devArray = json.getJSONArray("devices");
        for (int i = 0; i < devArray.length(); i++) {
            JSONObject obj = devArray.getJSONObject(i);
            String id = obj.getString("id");
            String ip = obj.getString("ip");
            int port = obj.getInt("port");
            devices.put(id, new DeviceInfo(id, ip, port));
            links.putIfAbsent(id, new ArrayList<>());
        }

        // Parse links
        JSONArray linkArray = json.getJSONArray("links");
        for (int i = 0; i < linkArray.length(); i++) {
            JSONArray pair = linkArray.getJSONArray(i);
            String a = pair.getString(0);
            String b = pair.getString(1);
            links.get(a).add(b);
            links.get(b).add(a);
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
}
