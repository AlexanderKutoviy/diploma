package attendance.daemon.template;

import org.json.JSONObject;

public interface Template {
    MailMessage formatMessage(JSONObject row);
}
