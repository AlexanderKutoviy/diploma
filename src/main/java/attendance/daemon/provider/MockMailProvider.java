package attendance.daemon.provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tit.utils.mapper.NameProperty;
import titanium.mail.sender.template.MailMessage;

public class MockMailProvider implements MailProvider {
    private final Logger l = LogManager.getLogger(getClass().getName() + "[thread:" + Thread.currentThread().getId() + "]");
    @NameProperty
    public long sleep = 4500;
    @NameProperty
    public boolean isStop = false;

    @Override
    public MailResponse sendMessage(MailMessage mailMessage) throws Exception {
        //l.trace("Provider sending message!");
        Thread.sleep(sleep);
        MailResponse mailResponse = new MailResponse();
        mailResponse.messageId = "abcd";
        mailResponse.state = "sent";
        mailResponse.responseBody = "body";
        if (isStop) {
            throw new Exception("Fail to send");
        } else {
            return mailResponse;
        }
    }
}
