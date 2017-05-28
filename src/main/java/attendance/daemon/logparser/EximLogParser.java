package attendance.daemon.logparser;


import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EximLogParser implements Daemon {
    @NameProperty
    public String filePath;
    @NameProperty
    public String namespace;
    public final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1, r -> new Thread(r, "ExecutorThread"));
    private SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private Logger l = LogManager.getLogger(getClass());
    SerializedSubject<String, String> subj = PublishSubject.<String>create().toSerialized();
    ComboPooledDataSource cpds;
    String lastTimestamp;
    Long prevTimestamp;
    Scheduler scheduler = Schedulers.from(executor);
    Thread thread;
    Tailer tailer;

    @Override
    public void run() throws Exception {
        final Pattern entryRegex = Pattern.compile("(?i)^(?<time>\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) (?<id>[a-z0-9]{6}-[a-z0-9]{6}-[a-z0-9]{2}) (?<msg>.*)$");
        try (Connection conn = cpds.getConnection()) {
            lastTimestamp = SqlQuery.create(conn, "SELECT TIMESTAMP(MAX(timestamp)) FROM ProviderMailEvent WHERE namespace=:serverId")
                    .set("serverId", namespace)
                    .queryString();
            if (lastTimestamp != null)
                lastTimestamp = lastTimestamp.substring(0, "2015-00-00 00:00:00".length());
            l.trace("Last timestamp: " + lastTimestamp);
        }

        subj.map((String t) -> toLogEntry(t, entryRegex))
                .filter((LogEntry t) -> t != null)
                .filter((LogEntry t) -> lastTimestamp == null || lastTimestamp.compareTo(t.timestamp) <= 0)
                .lift(new OperatorOnBackpressureGroup<>(scheduler))
                .subscribe((List<LogEntry> t) -> {
                    boolean retry = true;
                    while (retry)
                        try {
                            retry = false;
                            try (Connection conn = cpds.getConnection()) {
                                try {
                                    conn.setAutoCommit(false);
                                    for (LogEntry e : t) {
                                        long time = dFormat.parse(e.timestamp).getTime();
                                        if (prevTimestamp != null && time < prevTimestamp) {
                                            l.warn("Log timestamp {} is less than previous timestamp {}. Doublechek it.",
                                                    e.timestamp,
                                                    dFormat.format(new Date(prevTimestamp))
                                            );
                                            lastTimestamp = e.timestamp;
                                        }

                                        prevTimestamp = time;
                                        if (lastTimestamp != null && lastTimestamp.equals(e.timestamp)) {
                                            Integer has = SqlQuery.create(conn, "SELECT 1 FROM ProviderMailEvent " +
                                                    "WHERE namespace=:serverId " +
                                                    "AND mailId=:mailId " +
                                                    "AND timestamp=:timestamp " +
                                                    "AND message=:message")
                                                    .set("serverId", namespace)
                                                    .set("mailId", e.mailId)
                                                    .set("timestamp", e.timestamp)
                                                    .set("message", e.message)
                                                    .queryInt();
                                            if (has != null)
                                                continue;
                                        }
                                        new SqlSimpleInsert(conn, "ProviderMailEvent")
                                                .set("namespace", namespace)
                                                .set("mailId", e.mailId)
                                                .set("timestamp", e.timestamp)
                                                .set("message", e.message)
                                                .execute();
                                    }
                                    conn.commit();
                                } catch (Throwable e) {
                                    conn.rollback();
                                    throw e;
                                } finally {
                                    conn.setAutoCommit(true);
                                }
                            }
                        } catch (Exception ex) {
                            l.error("Error. Sleeping 30 sec before retry", ex);
                            retry = true;
                            try {
                                Thread.sleep(30000);
                            } catch (InterruptedException ex1) {
                                l.error("", ex);
                            }
                        }
                });
        File file = getAbsoluteFile(File.listRoots()[0], filePath);
        tailer = new Tailer(file, new TailerListener() {
            @Override
            public void init(Tailer tailer) {
                l.trace("Init tailer");
                try {
                    l.trace("File: " + tailer.getFile().getCanonicalPath());
                } catch (IOException ex) {
                    l.error("", ex);
                }
            }

            @Override
            public void fileNotFound() {
                l.trace("File not found");
            }

            @Override
            public void fileRotated() {
                l.trace("File rotated");
            }

            @Override
            public void handle(String line) {
                subj.onNext(line);
            }

            @Override
            public void handle(Exception ex) {
                l.error("Tailer error", ex);
            }

        }, 500, false, false, 10000);

        thread = new Thread(tailer);
        thread.start();
    }

    @Override
    public void stop() throws Exception {
        thread.interrupt();
        tailer.stop();
    }

    @Override
    public void setCpds(ComboPooledDataSource cpds) {
        this.cpds = cpds;
    }

    @Override
    public boolean isStopped() {
        return thread.isInterrupted();
    }

    public static class LogEntry {
        public String timestamp;
        public String mailId;
        public String message;

        @Override
        public String toString() {
            return "LogEntry{" + "timestamp=" + timestamp + ", mailId=" + mailId + ", message=" + message + '}';
        }
    }

    private LogEntry toLogEntry(String t, Pattern entryRegex) {
        Matcher m = entryRegex.matcher(t);
        if (m.matches()) {
            LogEntry e = new LogEntry();
            e.timestamp = m.group("time");
            e.mailId = m.group("id");
            e.message = m.group("msg");
            return e;
        }
        return null;
    }

    private File getAbsoluteFile(File root, String path) {
        File file = new File(path);
        if (file.isAbsolute())
            return file;

        if (root == null)
            return null;

        return new File(root, path);
    }
}