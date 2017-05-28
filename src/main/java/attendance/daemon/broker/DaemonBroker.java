package attendance.daemon.broker;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import tit.db.SqlQuery;
import tit.utils.mapper.InvalidValueException;
import tit.utils.mapper.MissingPropertyException;
import tit.utils.mapper.PropertiesMapper;
import titanium.mail.sender.dao.DaemonModel;

import javax.inject.Inject;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DaemonBroker {

    private static final long PERIOD_OF_BAN = 1_000_000_000L * 60;
    final ComboPooledDataSource cpds;
    final ConcurrentHashMap<Integer, DaemonHolder> daemonHolders = new ConcurrentHashMap<>();
    final ConcurrentHashMap<Integer, Long> daemonBan = new ConcurrentHashMap<>();
    private final Logger l = LogManager.getLogger(getClass().getName() + "[thread:" + Thread.currentThread().getId() + "]");
    long lastReload = 0;
    private Thread brokerThread;

    @Inject
    public DaemonBroker(ComboPooledDataSource cpds) {
        this.cpds = cpds;
    }

    public void start() {
        brokerThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try (Connection conn = cpds.getConnection()) {
                        Thread.sleep(200);
                        tick(conn);
                    } catch (Throwable e) {
                        l.error("Interrupted exception in TaskBroker.start()", e);
                        try {
                            Thread.sleep(30000);
                        } catch (InterruptedException e1) {
                            l.trace("Interrupted ", e1);
                            return;
                        }
                    }
                }
            }
        };
        brokerThread.start();
    }

    public void stop() {
        l.trace("DaemonBroker {} stop", brokerThread.getId());
        brokerThread.interrupt();
    }

    protected void tick(Connection connection) throws Exception {
        //update parser holders map
        if (System.nanoTime() - lastReload > 1000000000L * 10) {
            reloadDaemons(connection);
            lastReload = System.nanoTime();
        }
        startDaemons();
    }

    private void startDaemons() throws Exception {
        for (DaemonHolder parserHolder : daemonHolders.values()) {
            if (parserHolder.daemon == null) {
                parserHolder.daemon = newInstanceFromJson(parserHolder.daemonModel.config);
                parserHolder.state = DaemonHolder.State.RUNNING;
                parserHolder.daemon.setCpds(cpds);
                parserHolder.daemon.run();
            }

            if (parserHolder.daemon != null && parserHolder.state == DaemonHolder.State.STOPPING) {
                parserHolder.daemon.stop();
                if (parserHolder.daemon.isStopped()) {
                    parserHolder.daemon = newInstanceFromJson(parserHolder.daemonModel.config);
                    parserHolder.state = DaemonHolder.State.RUNNING;
                    parserHolder.daemon.setCpds(cpds);
                    parserHolder.daemon.run();
                }
            }
        }
    }

    public static <T> T newInstanceFromJson(JSONObject json) throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, MissingPropertyException, InvalidValueException {
        Class<?> cls = Class.forName(json.getString("class"));
        T obj = (T) cls.newInstance();
        PropertiesMapper.jsonToObject(obj, json);
        return obj;
    }

    private void reloadDaemons(Connection connection) throws Exception {
        l.trace("Reload daemons");

        List<DaemonModel> daemonModels = SqlQuery.create(connection, "SELECT * FROM ProviderDaemon")
                .queryObjects(DaemonModel.class);

        long now = System.nanoTime();
        for (DaemonModel daemonModel : daemonModels) {
            Long bannedTill = daemonBan.get(daemonModel.id);
            if (bannedTill != null) {
                if (now - bannedTill < 0) {
                    continue;
                } else {
                    daemonBan.remove(daemonModel.id);
                }
            }
            DaemonHolder holder = daemonHolders.get(daemonModel.id);

            if (holder == null) {
                l.trace("Create daemon holder for {}", daemonModel);
                holder = new DaemonHolder(daemonModel);
                daemonHolders.put(daemonModel.id, holder);
            } else if (JSONCompare.compareJSON(holder.daemonModel.config, daemonModel.config, JSONCompareMode.LENIENT)
                    .failed()) {
                l.trace("Replace daemon holder (old : {}, new : {})", holder.daemonModel, daemonModel);
                holder.state = DaemonHolder.State.STOPPING;
                holder.newHolder = new DaemonHolder(daemonModel);
            }
        }
    }
}