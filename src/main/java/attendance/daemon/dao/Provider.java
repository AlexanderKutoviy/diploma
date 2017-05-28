package attendance.daemon.dao;

import org.json.JSONObject;

public class Provider {
    public int id;
    public String name;
    public JSONObject config;
    public String from;

    @Override
    public String toString() {
        return "Provider{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", config=" + config +
                ", from='" + from + '\'' +
                '}';
    }
}
