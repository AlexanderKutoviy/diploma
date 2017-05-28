package attendance.daemon.provider;


import titanium.mail.sender.template.MailMessage;

public interface MailProvider {
    default boolean isAlive() {
        return true;
    }

    /**
     * Can throw exception only if message is not sent for sure
     * <p>
     * If error occurs after the mail is sent, error message should be returned in getSentMessageError
     */
    MailResponse sendMessage(MailMessage mailMessage) throws Exception;
}
