package attendance.daemon.broker;

import attendance.daemon.worker.MailSenderWorker;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.oracle.jrockit.jfr.InvalidValueException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;

import javax.inject.Inject;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.osgi.framework.AdminPermission.CLASS;

public class TaskBroker {
    private static final int TASK_LIMIT = 10;
    static long PERIOD_OF_BAN = 1_000_000_000L * 60;
    final ComboPooledDataSource cpds;
    final ConcurrentHashMap<Integer, ProviderHolder> providerHolders = new ConcurrentHashMap<>();
    final ConcurrentHashMap<Integer, TaskHolder> taskHolders = new ConcurrentHashMap<>();
    final HashMap<MailSenderWorker, TaskSet> workersTasks = new HashMap<>();
    final ConcurrentHashMap<Integer, Long> providerBan = new ConcurrentHashMap<>();
    private final Logger l = LogManager.getLogger(getClass().getName() + "[thread:" + Thread.currentThread().getId() + "]");
    long lastReload = 0;
    private Thread brokerThread;

    @Inject
    public TaskBroker(ComboPooledDataSource cpds) {
        this.cpds = cpds;
    }

    static <T> T newInstanceFromJson(JSONObject json) throws ClassNotFoundException, IllegalAccessException, InstantiationException, MissingPropertyException, InvalidValueException {
        Class<?> cls = Class.forName(json.getString(CLASS));
        T obj = (T) cls.newInstance();
        PropertiesMapper.jsonToObject(obj, json);
        return obj;
    }

    public void markWorkerFinished(MailSenderWorker worker) {
        synchronized (workersTasks) {
            workersTasks.remove(worker);
        }
    }

    public TaskSet getNewTaskSet(MailSenderWorker worker) throws InterruptedException {
        markWorkerFinished(worker);

        TaskSet task;
        ProviderHolder handlerProvider = null;

        while (true) {
            if (!providerHolders.isEmpty()) {
                handlerProvider = providerHolders
                    .values()
                    .stream()
                    .filter(p -> p.tasks.size() > 0)
                    .filter(providerHolder -> (providerHolder.provider != null))
                    .filter(providerHolder -> providerHolder.provider.isAlive())
//                        .sorted(Comparator.comparing(h -> h.lastModified))
                    .sorted((holder1, holder2) -> {
                        if (holder1.lastModified == null || holder2.lastModified == null) {
                            return 1;
                        } else {
                            return holder2.lastModified.compareTo(holder1.lastModified);
                        }
                    })
                    .findFirst()
                    .orElse(null);
            }

            if (handlerProvider != null) {
                task = handlerProvider.tasks.poll(1, TimeUnit.SECONDS);
                handlerProvider.lastModified = new Date();

                if (task != null) {
                    TaskHolder holder = taskHolders.get(task.taskId);
                    if (holder.state != TaskHolder.State.STOPPING)
                        break;
                }
            } else
                Thread.sleep(300);
        }
        synchronized (workersTasks) {
            workersTasks.put(worker, task);
        }
        return task;
    }

    public void stopProvider(int providerId) {
        if (providerBan.get(providerId) == null) {
            l.trace("Ban provider {} ", providerId);
        }
        providerHolders.get(providerId).state = ProviderHolder.State.STOPPING;
        providerBan.put(providerId, System.nanoTime() + PERIOD_OF_BAN);
    }

    public void stopWorking() {
        l.trace("Broker {} stop", brokerThread.getId());
        brokerThread.interrupt();
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

    void tick(Connection conn) throws Exception {
        long now = System.nanoTime();

        if (now - lastReload > 1000000000L * 10) {
            for (ProviderHolder providerHolder : providerHolders.values()) {
                if (providerHolder.state.equals(ProviderHolder.State.STOPPING)) {
                    boolean hasAnyTask = taskHolders.values().stream()
                        .filter(h -> h.task.provider == providerHolder.providerModel.id)
                        .findAny()
                        .isPresent();

                    if (!hasAnyTask) {
                        if (providerHolder.newHolder == null) {
                            l.trace("ProviderHolder {} has no tasks. REMOVE", providerHolder.providerModel.id);
                            providerHolders.remove(providerHolder.providerModel.id);
                        } else {
                            l.trace("ProviderHolder {} has no tasks. REPLACE", providerHolder.providerModel.id);
                            providerHolders.put(providerHolder.providerModel.id, providerHolder.newHolder);
                        }
                    }
                }
            }

            reloadTasks(conn);
            reloadProviders(conn);
            lastReload = System.nanoTime();
        }

        for (ProviderHolder providerHolder : providerHolders.values()) {
            if (providerHolder.provider == null) {
                providerHolder.provider = newInstanceFromJson(providerHolder.providerModel.config);
            }

            if (providerHolder.state.equals(ProviderHolder.State.STOPPING)) {
                providerHolder.tasks.forEach(taskSet -> taskHolders.get(taskSet.taskId).state = TaskHolder.State.STOPPING);
            }
        }

        {
            if (taskHolders.isEmpty() && !providerHolders.isEmpty()) {
                providerHolders.clear();
                l.trace("There no taskHolders, remove providerHolders");
            } else {
                HashSet<Integer> ids = new HashSet<>(providerHolders.keySet());
                ids.removeAll(
                    taskHolders.values().stream()
                        .map(h -> h.task.provider)
                        .collect(Collectors.toList())
                );

                ids.forEach(id -> {
                    if (providerHolders.get(id).state != ProviderHolder.State.STOPPING) {
                        providerHolders.get(id).state = ProviderHolder.State.STOPPING;
                        l.trace("ProviderHolder {} has no tasks. Make it STOPPING!", id);
                    }
                });
            }
        }

        for (TaskHolder taskHolder : taskHolders.values()) {
            try {
                Task task = taskHolder.task;
                ProviderHolder providerHolder = providerHolders.get(task.provider);

                if (providerHolder == null)
                    continue;

                if (!task.state.equals(Task.State.RUNNING.name)) {
                    l.trace("Mark task {} as RUNNING", task.id);
                    SqlSimpleUpdate taskUpdate = new SqlSimpleUpdate(conn, "Task", " id=" + task.id);
                    taskUpdate.set("state", Task.State.RUNNING.name);
                    taskUpdate.execute();
                    task.state = Task.State.RUNNING.name;
                }

                if (taskHolder.ids == null) {
                    l.trace("Build task {} ids list", task.id);

                    taskHolder.fetchIds(conn);

                    if (taskHolder.ids.isEmpty()) {
                        l.trace("Mark task {} as DONE", task.id);
                        new SqlSimpleUpdate(conn, "Task", " id=" + task.id)
                            .set("state", Task.State.DONE.name)
                            .execute();
                        taskHolders.remove(taskHolder.task.id);
                        continue;
                    }
                }

                if (taskHolder.state == TaskHolder.State.STOPPING) {
                    providerHolder.tasks.removeIf(taskSet -> taskSet.taskId == taskHolder.task.id);

                    boolean isProcessing = false;
                    synchronized (workersTasks) {
                        for (Map.Entry<MailSenderWorker, TaskSet> e : workersTasks.entrySet()) {
                            if (e.getValue().taskId == taskHolder.task.id)
                                isProcessing = true;
                        }
                    }

                    if (!isProcessing) {
                        l.trace("TaskHolder {} finish stopping ", taskHolder.task.id);

                        if (taskHolder.newHolder == null || providerHolder.state != ProviderHolder.State.STOPPING) {
                            taskHolders.remove(taskHolder.task.id);
                            l.trace("TaskHolder {} REMOVED", taskHolder.task.id);
                        } else {
                            l.trace("TaskHolder {} REPLACED", taskHolder.task.id);
                            taskHolders.put(taskHolder.task.id, taskHolder.newHolder);
                        }
                    }
                    continue;
                }

                if (taskHolder.ids.isEmpty()
                    &&
                    !providerHolder.tasks.stream()
                        .filter(taskSet -> taskSet.taskId == taskHolder.task.id)
                        .findAny()
                        .isPresent()
                    ) {
                    l.trace("TaskHolder {} has no more new ids. STOPPING", taskHolder.task.id);
                    taskHolder.state = TaskHolder.State.STOPPING;
                    continue;
                }

                if (taskHolder.ids.isEmpty())
                    continue;

                List<String> ids = taskHolder.ids
                    .stream()
                    .limit(TASK_LIMIT)
                    .collect(Collectors.toList());
                taskHolder.ids.removeAll(ids);

                TaskSet res = new TaskSet();
                res.taskId = task.id;
                res.template = newInstanceFromJson(task.data.getJSONObject(TaskHolder.TEMPLATE));
                res.rows = taskHolder.getDataset().getRowsByIds(ids);
                res.providerHolder = providerHolder;
                providerHolder.tasks.put(res);

                l.trace("New TaskSet added ({} rows). Provider {}. TaskSets {}. Ids left {}",
                    res.rows.size(),
                    providerHolder.providerModel.id,
                    providerHolder.tasks.size(),
                    taskHolder.ids.size()
                );
            } catch (Throwable e) {
                l.error("", e);
            }
        }
    }

    private void reloadTasks(Connection conn) throws Exception {
        l.trace("Reload tasks. Tasks in taskHolders {}, workerTasks {}.", taskHolders.size(), workersTasks.size());
        List<Task> tasks = SqlQuery.create(conn,
            "SELECT * FROM Task WHERE (state=:todoState OR state=:runningState)" +
                "  ORDER BY timeLastProcessed ASC")
            .set("todoState", Task.State.NEW.name)
            .set("runningState", Task.State.RUNNING.name)
            .queryObjects(Task.class);

        for (Task task : tasks) {
            ProviderHolder pHolder = providerHolders.get(task.provider);
            if (pHolder != null && pHolder.state == ProviderHolder.State.STOPPING)
                continue;

            TaskHolder holder = taskHolders.get(task.id);

            if (holder == null) {
                l.trace("Create holder for {}", task);
                holder = new TaskHolder(task);
                taskHolders.put(task.id, holder);
            } else if (JSONCompare.compareJSON(holder.task.data, task.data, JSONCompareMode.LENIENT).failed()) {
                l.trace("Replace holder (old task: {}, new task: {})", holder.task, task);
                holder.state = TaskHolder.State.STOPPING;
                holder.newHolder = new TaskHolder(task);
            }
        }
    }

    private void reloadProviders(Connection conn) throws Exception {
        l.trace("Reload providers. Providers in providerHolders {}, workerTasks {}.", providerHolders.size(), workersTasks.size());

        Set<Integer> ids = taskHolders.values()
            .stream()
            .map(h -> h.task.provider)
            .collect(Collectors.toSet());

        if (ids.isEmpty())
            return;

        List<Provider> providers = SqlQuery.create(conn, " SELECT * FROM Provider WHERE id in " +
            "(" + ids.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")")
            .queryObjects(Provider.class);

        long now = System.nanoTime();

        for (Provider provider : providers) {
            Long bannedTill = providerBan.get(provider.id);
            if (bannedTill != null) {
                if (now - bannedTill < 0) {
                    continue;
                } else {
                    l.trace("Remove {} provider from ban.", provider.id);
                    providerBan.remove(provider.id);
                }
            }
            ProviderHolder holder = providerHolders.get(provider.id);

            if (holder == null) {
                l.trace("Create provider holder for {}", provider);
                holder = new ProviderHolder(provider);
                providerHolders.put(provider.id, holder);
            } else if (JSONCompare.compareJSON(holder.providerModel.config, provider.config, JSONCompareMode.LENIENT).failed()
                || !Objects.equals(holder.providerModel.from, provider.from)) {
                l.trace("Replace provider holder (old : {}, new : {})", holder.providerModel, provider);
                holder.state = ProviderHolder.State.STOPPING;
                holder.newHolder = new ProviderHolder(provider);
            }
        }
    }
}