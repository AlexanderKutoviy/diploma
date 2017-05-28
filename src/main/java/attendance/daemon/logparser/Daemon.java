package attendance.daemon.logparser;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public interface Daemon {

    void run() throws Exception;

    void stop() throws Exception;

    void setCpds(ComboPooledDataSource cpds);

    boolean isStopped();
}