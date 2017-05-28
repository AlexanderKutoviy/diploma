package attendance.daemon.provider.smtp;

import com.sun.mail.smtp.SMTPTransport;
import org.apache.commons.lang3.StringUtils;
import tit.utils.mapper.NameProperty;
import titanium.mail.sender.provider.MailProvider;
import titanium.mail.sender.provider.MailResponse;
import titanium.mail.sender.template.MailMessage;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SmtpMailProvider implements MailProvider {

    private static final String ENCODING = "UTF-8";
    private static final String SMTP = "smtp";
    private static final String PORT = "mail.smtp.port";
    private static final String HOST = "mail.smtp.host";
    private static final String AUTH = "mail.smtp.auth";
    private static final String CHARSET = "mail.smtp.charset";
    @NameProperty
    public String smtpHost;
    @NameProperty
    public String login;
    @NameProperty
    public String password;
    @NameProperty
    public String smtpPort;
    private Properties properties;
    private Authenticator authenticator;
    private Session session;

    @Override
    public MailResponse sendMessage(MailMessage msg) throws Exception {
        synchronized (this) {
            if (properties == null) {
                properties = setupProperties(System.getProperties());
            }
            if (authenticator == null) {
                authenticator = new TitaniumAuthenticator(login, password);
            }
            if (session == null) {
                session = Session.getInstance(properties, authenticator);
                session.setDebug(true);
            }
        }

        MimeMessage message = composeMessage(msg);
        SMTPTransport transport = null;
        try {
            transport = (SMTPTransport) session.getTransport(SMTP);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.writeTo(baos);

            transport = (SMTPTransport) session.getTransport(SMTP);
            transport.connect();
            transport.sendMessage(message, message.getAllRecipients());
            String response = transport.getLastServerResponse();

            MailResponse mailResponse = new MailResponse();
            mailResponse.messageBody = new String(baos.toByteArray());
            mailResponse.messageId = getMessageIdFromResponse(response);
            mailResponse.responseBody = response;

            if (StringUtils.isNotBlank(mailResponse.messageId))
                mailResponse.state = "queued";

            return mailResponse;
        } finally {
            if (transport != null)
                transport.close();
        }

    }

    private MimeMessage composeMessage(MailMessage msg) throws Exception {
        MimeBodyPart htmlPart = null;
        MimeBodyPart plainPart = null;
        MimeMultipart relatedPart = null;
        MimeMultipart alternativePart = null;
        MimeMultipart mixedPart = null;
        boolean hasRelated = msg.attachments.stream().filter(a -> a.isRelated).findAny().isPresent();
        boolean hasMixed = msg.attachments.stream().filter(a -> !a.isRelated).findAny().isPresent();
        boolean hasHtml = msg.contentHtml != null;
        boolean hasPlain = msg.contentText != null;

        MimeMessage res = new MimeMessage(session);
        res.setFrom(new InternetAddress(msg.from));
        InternetAddress[] internetAddresses = InternetAddress.parse(recipientsToString(msg.recipients), true);
        res.setRecipients(Message.RecipientType.TO, internetAddresses);
        res.setSubject(msg.subject, ENCODING);

        if (!hasHtml && !hasPlain) {
            throw new Exception("Mail should have text or html");
        }

        if (hasHtml) {
            htmlPart = new MimeBodyPart();
            htmlPart.setContent(msg.contentHtml, "text/html; charset=utf-8");
            if (hasRelated) {
                relatedPart = new MimeMultipart("related");
                relatedPart.addBodyPart(htmlPart);
                for (MailMessage.Attachment a : msg.attachments)
                    if (a.isRelated) {
                        MimeBodyPart part = new MimeBodyPart();
                        DataSource source = new ByteArrayDataSource(a.data, a.mimeType);
                        part.setDataHandler(new DataHandler(source));
                        part.setHeader("Content-Disposition", "inline");
                        if (a.cid != null) {
                            part.setContentID("<" + a.cid + ">");
                        }
                        relatedPart.addBodyPart(part);
                    }
            }
        }

        if (hasPlain) {
            plainPart = new MimeBodyPart();
            plainPart.setContent(msg.contentText, "text/plain; charset=utf-8");
        }

        if (hasPlain && hasHtml) {
            alternativePart = new MimeMultipart("alternative");
            alternativePart.addBodyPart(plainPart);
            if (hasRelated) {
                BodyPart b = new MimeBodyPart();
                alternativePart.addBodyPart(b);
                b.setContent(relatedPart);
            } else {
                alternativePart.addBodyPart(htmlPart);
            }
        }

        if (hasMixed) {
            mixedPart = new MimeMultipart();
            if (alternativePart != null) {
                BodyPart b = new MimeBodyPart();
                mixedPart.addBodyPart(b);
                b.setContent(alternativePart);
            } else if (relatedPart != null) {
                BodyPart b = new MimeBodyPart();
                mixedPart.addBodyPart(b);
                b.setContent(relatedPart);
            } else if (htmlPart != null) {
                mixedPart.addBodyPart(htmlPart);
            } else {
                mixedPart.addBodyPart(plainPart);
            }

            for (MailMessage.Attachment a : msg.attachments) {
                if (!a.isRelated || htmlPart == null) {
                    MimeBodyPart part = new MimeBodyPart();
                    DataSource source = new ByteArrayDataSource(a.data, a.mimeType);
                    part.setDataHandler(new DataHandler(source));
                    mixedPart.addBodyPart(part);
                }
            }
        }

        if (mixedPart != null) {
            res.setContent(mixedPart);
        } else if (alternativePart != null) {
            res.setContent(alternativePart);
        } else if (relatedPart != null) {
            res.setContent(relatedPart);
        } else if (htmlPart != null) {
            res.setDataHandler(htmlPart.getDataHandler());
        } else {
            res.setDataHandler(plainPart.getDataHandler());
        }
        return res;
    }

    private String getMessageIdFromResponse(String responseBody) {
        if (responseBody != null) {
            String[] parts = responseBody.split("=");
            return parts[1];
        } else {
            return "";
        }
    }

    private Properties setupProperties(Properties properties) {
        properties.put(PORT, smtpPort);
        properties.put(HOST, smtpHost);
        properties.put(AUTH, "true");
        properties.put(CHARSET, ENCODING);
        return properties;
    }

    private static String recipientsToString(List<String> recipients) {
        String result = "";
        for (int i = 0; i < recipients.size() - 1; i++) {
            result += recipients.get(i) + ", ";
        }

        return result + recipients.get(recipients.size() - 1);
    }

    public static void main(String[] args) throws Exception {
        List<MailMessage> mailMessages = new ArrayList<>();
        MailMessage m = new MailMessage();
        m.from = "bot@therespo.com";
//        m.addRecipient("igor@ert.org.ua");
        m.addRecipient("kutoviyo@gmail.com");
//        m.addRecipient("kutoviy12@ukr.net");
        if (true) {
            m.subject = "HTML йцукен";
            m.contentText = "qrtyuiop йцукен";
        }
        if (false) {
            m.subject = "HTML йцукен";
            m.contentHtml = "<b>blod йцукен</b>";
        }
        if (false) {
            m.subject = "HTML йцукен";
            m.contentText = "IMG";
            m.addAttachment();
        }
        if (false) {
            m.subject = "HTML йцукен";
            m.contentText = "IMG";
            m.addAttachment();
        }
        if (false) {
            m.subject = "HTML йцукен";
            MailMessage.Attachment a = m.addAttachment();
            Path path = Paths.get("/home/olexandr/attach.jpg");
            a.mimeType = "image/jpg";
            a.isRelated = true;
            try {
                a.data = Files.readAllBytes(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            m.contentHtml = "<img src='cid:" + a.ensureCid() + "'>";
        }
        mailMessages.add(m);
        SmtpMailProvider p = new SmtpMailProvider();
        p.smtpHost = "halk.min.org.ua";
        p.smtpPort = "587";
        p.login = "bot@therespo.com";
        p.password = "P4HB8NS1Tk";
        p.sendMessage(mailMessages.get(0));
    }
}