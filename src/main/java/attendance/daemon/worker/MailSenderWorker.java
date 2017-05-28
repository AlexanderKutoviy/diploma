package attendance.daemon.worker;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import tit.db.SqlSimpleInsert;
import tit.db.SqlSimpleUpdate;
import tit.db.SqlUtils;
import titanium.mail.sender.broker.TaskBroker;
import titanium.mail.sender.broker.TaskSet;
import titanium.mail.sender.dao.MailLog;
import titanium.mail.sender.provider.MailProvider;
import titanium.mail.sender.provider.MailResponse;
import titanium.mail.sender.template.MailMessage;

import javax.inject.Inject;
import java.sql.Connection;
import java.util.Map;

public class MailSenderWorker {
    private final Logger logger = LogManager.getLogger(getClass().getName() + "[thread:" + Thread.currentThread().getId() + "]");
    private final TaskBroker broker;
    private final ComboPooledDataSource cpds;
    private long threadId;

    @Inject
    public MailSenderWorker(TaskBroker broker, ComboPooledDataSource cpds) {
        this.broker = broker;
        this.cpds = cpds;
    }

    public void tick() throws Exception {
        TaskSet taskSet = broker.getNewTaskSet(this);
        tick(taskSet);
    }

    public void stop() {
        logger.trace("Worker stop {}", threadId);
        Thread.currentThread().interrupt();
    }

    public void tick(TaskSet taskSet) throws Exception {
        logger.trace("Task: " + taskSet);
        for (Map.Entry<String, JSONObject> entry :
            taskSet.rows.entrySet()) {
            JSONObject row = entry.getValue();
            MailMessage msg = taskSet.template.formatMessage(row);

            logger.trace("I'm sending {}", msg);
            MailLog mailLog = new MailLog();
            mailLog.taskId = taskSet.taskId;
            mailLog.rowId = entry.getKey();
            mailLog.address = row.getString("address");
            mailLog.data = new JSONObject();

            try (Connection conn = cpds.getConnection()) {
                new SqlSimpleInsert(conn, "MailLog")
                    .setFromObject(mailLog, "taskId", "rowId", "address")
                    .execute();

                mailLog.id = SqlUtils.getLastInsertId(conn);
            }

            boolean failed = false;
            try {
                MailProvider provider = taskSet.providerHolder.provider;
                mailLog.provider = taskSet.providerHolder.providerModel.id;

                try {
                    logger.trace("Provider {} start sending message ", taskSet.providerHolder.providerModel.id);
                    MailResponse res = provider.sendMessage(msg);
                    mailLog.body = res.messageBody;
                    mailLog.providerMessageId = res.messageId;
                    mailLog.providerState = res.state;
                    if (res.error != null) {
                        mailLog.data.put("error", res.error);
                    }
                    if (res.responseBody != null) {
                        mailLog.data.put("response", res.responseBody);
                    }
                } catch (Exception e) {
                    logger.error("", e);
                    mailLog.data.put("exception", e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
                    mailLog.providerState = MailLog.STATE_FAILED_TO_SEND;
                    broker.stopProvider(taskSet.providerHolder.providerModel.id);
                    failed = true;
                }

                try (Connection conn = cpds.getConnection()) {
                    new SqlSimpleUpdate(conn, "MailLog", " id=" + mailLog.id)
                        .setFromObject(mailLog, "body", "data", "provider", "providerMessageId", "providerState")
                        .execute();
                }
                if (failed) {
                    return;
                }

            } catch (Throwable e) {
                logger.error("", e);

                mailLog.providerState = "exception";
                mailLog.data.put("exception", e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
                try (Connection conn = cpds.getConnection()) {
                    new SqlSimpleUpdate(conn, "MailLog", " id=" + mailLog.id)
                        .setFromObject(mailLog, "providerState", "data")
                        .execute();
                }
            }
        }
    }

    public void start() {
        logger.trace("Started");
        threadId = Thread.currentThread().getId();
        while (true) {
            try {
                tick();
            } catch (Throwable e) {
                logger.error("Interrupted in worker.started ", e);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e1) {
                    logger.trace("Interrupted");
                    return;
                }
            }
        }
    }
}