package attendance.daemon.broker;

import titanium.mail.sender.dao.DaemonModel;
import titanium.mail.sender.logparser.Daemon;

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