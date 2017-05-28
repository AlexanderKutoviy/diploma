package attendance.daemon.template;

import org.json.JSONObject;
import tit.utils.mapper.NameProperty;

import java.util.Collections;

public class MockTemplate implements Template {

    @NameProperty
    public String subject;

    @Override
    public MailMessage formatMessage(JSONObject row) {
        MailMessage res = new MailMessage();
        res.recipients = Collections.singletonList(row.getString("address"));
        res.from = "bot@ert.org.ua";
        res.subject = subject;
        res.contentText = "Hello! привіт";
        return res;
    }
}
