package attendance.daemon.broker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tit.db.SqlQuery;
import tit.db.SqlSimpleUpdate;
import titanium.mail.sender.dao.MailLog;
import titanium.mail.sender.dao.Task;
import titanium.mail.sender.dataset.Dataset;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;

public class TaskHolder {
    private static final Logger l = LogManager.getLogger(TaskHolder.class);

    public static final String DATASET = "dataset";
    public static final String TEMPLATE = "template";

    public final Task task;

    private Dataset dataset;

    public TaskHolder newHolder;

    public TaskHolder(Task task) {
        this.task = task;
    }

    HashSet<String> ids;

    public State state = State.RUNNING;

    public enum State {
        RUNNING(),
        STOPPING()
    }

    public void fetchIds(Connection conn) throws Exception {
        SqlSimpleUpdate taskUpdate = new SqlSimpleUpdate(conn, "Task", " id=" + task.id);
        taskUpdate.set("timeLastProcessed", SqlSimpleUpdate.CURRENT_TIMESTAMP);
        taskUpdate.execute();

        this.ids = new HashSet<>(getDataset().getIds());
        ArrayList<String> sentIds = SqlQuery.create(conn, "SELECT rowId FROM MailLog WHERE taskId=:task " +
                "AND providerState<>:failedState")
                .set("task", task.id)
                .set("failedState", MailLog.STATE_FAILED_TO_SEND)
                .queryStrings();

        this.ids.removeAll(sentIds);

    }

    public Dataset getDataset() throws Exception {
        if (dataset == null)
            dataset = TaskBroker.newInstanceFromJson(task.data.getJSONObject(DATASET));
        return dataset;
    }
}
