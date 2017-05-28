package attendance.daemon.dao;

import org.json.JSONObject;

public class DaemonModel {
    public int id;
    public String name;
    public JSONObject config;

    @Override
    public String toString() {
        return "DaemonModel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", config=" + config +
                '}';
    }
}