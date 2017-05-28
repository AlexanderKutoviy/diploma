package attendance.daemon.broker;

import attendance.daemon.dao.DaemonModel;
import attendance.daemon.logparser.Daemon;

public class DaemonHolder {

    public final DaemonModel daemonModel;
    public Daemon daemon;
    public State state = State.RUNNING;
    public DaemonHolder newHolder;

    public DaemonHolder(DaemonModel logParserModel) {
        this.daemonModel = logParserModel;
    }

    public enum State {
        RUNNING(),
        STOPPING()
    }
}