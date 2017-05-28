package attendance.daemon.inspector;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.sun.mail.imap.IMAPFolder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tit.db.SqlQuery;
import tit.utils.mapper.NameProperty;
import titanium.mail.sender.logparser.Daemon;
import titanium.mail.sender.provider.smtp.TitaniumAuthenticator;

import javax.mail.*;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.util.Properties;

public class IMAPMailInspector implements Daemon {

    private static final String PROTOCOL = "mail.store.protocol";
    private static final String HOST = "mail.imap.host";
    private static final String PORT = "mail.imap.port";
    private static final String USER = "mail.imap.user";
    private static final String STORE_TYPE = "imaps";
    @NameProperty
    public String host;
    @NameProperty
    public String port;
    @NameProperty
    public String user;
    @NameProperty
    public String password;
    private Properties properties;
    private Authenticator authenticator;
    private Session session;
    private ComboPooledDataSource cpds;
    private Logger l = LogManager.getLogger(getClass());
    private Thread thread;

    @Override
    public void run() {
        thread = new Thread(() -> {
            try {
                fetch();
            } catch (Exception e) {
                l.error("", e);
            }
        });
        thread.start();
    }

    @Override
    public void stop() throws Exception {
        thread.interrupt();
    }

    @Override
    public void setCpds(ComboPooledDataSource cpds) {
        this.cpds = cpds;
    }

    @Override
    public boolean isStopped() {
        return thread.isInterrupted();
    }

    private void fetch() throws Exception {
        IMAPFolder folder;
        Store store;
        synchronized (this) {
            if (properties == null) {
                properties = setupProperties(System.getProperties());
            }
            if (authenticator == null) {
                authenticator = new TitaniumAuthenticator(user, password);
            }
            if (session == null) {
                session = Session.getInstance(properties, authenticator);
                session.setDebug(true);
            }
        }
        store = session.getStore(STORE_TYPE);
        store.connect(host, user, password);
        folder = (IMAPFolder) store.getFolder("inbox");

        if (!folder.isOpen()) {
            folder.open(Folder.READ_WRITE);
        }
        Message[] messages = folder.getMessages();

        for (Message msg : messages) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            msg.writeTo(baos);
            try (Connection conn = cpds.getConnection()) {
                SqlQuery.create(conn, "INSERT INTO Inbox(`subject`, `from`, `to`, `date`, `size`,`fingerprint`, `body`)" +
                        "VALUES(:subject, :from, :to, :date, :size, :fingerprint,COMPRESS(:body))")
                        .set("subject", msg.getSubject())
                        .set("from", msg.getFrom()[0].toString())
                        .set("to", msg.getAllRecipients()[0].toString())
                        .set("date", msg.getReceivedDate())
                        .set("size", msg.getSize())
                        .set("fingerprint", Long.toString(msg.getReceivedDate().getTime() / 1000, 36) +
                                ":" + DigestUtils.sha1Hex(baos.toByteArray()))
                        .set("body", baos.toByteArray())
                        .executeUpdate();
                msg.setFlag(Flags.Flag.DELETED, true);
            }
        }

        if (folder.isOpen()) {
            folder.close(true);
        }
        store.close();
    }

    private Properties setupProperties(Properties properties) {
        properties.setProperty(PROTOCOL, "imaps");
        properties.setProperty(HOST, host);
        properties.setProperty(PORT, port);
        properties.setProperty(USER, "true");
        return properties;
    }
}
