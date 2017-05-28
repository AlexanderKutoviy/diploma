package attendance.daemon.broker;

import org.json.JSONObject;
import titanium.mail.sender.template.Template;

import java.util.Map;

public class TaskSet {

    public int taskId;

    public Template template;

    public ProviderHolder providerHolder;

    /**
     * String - id => JSONObject - row
     */
    public Map<String, JSONObject> rows;

    @Override
    public String toString() {
        return "TaskSet{" +
                "template=" + template +
                ", rows=" + rows +
                '}';
    }
}