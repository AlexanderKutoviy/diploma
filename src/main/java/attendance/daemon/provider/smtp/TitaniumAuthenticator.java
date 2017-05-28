package attendance.daemon.provider.smtp;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class TitaniumAuthenticator extends Authenticator {

    private String user;
    private String password;

    public TitaniumAuthenticator(String user, String password) {
        this.user = user;
        this.password = password;
    }

    @Override
    public PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(this.user, this.password);
    }
}