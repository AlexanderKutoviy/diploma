package attendance.daemon.dao;

import org.json.JSONObject;
import titanium.mail.sender.provider.MailResponse;

import java.util.Date;

public class MailLog {

    public static final String STATE_FAILED_TO_SEND = "failed-to-send";

    public int id;
    public int taskId;
    public String rowId;
    public Date timeSent;
    public String body;
    public String address;
    public JSONObject data;
    public Integer provider;
    public String providerMessageId;
    public String providerState;
    public String error;

    public MailLog() {
    }

    public MailLog(MailResponse mailResponse) {

    }
}
