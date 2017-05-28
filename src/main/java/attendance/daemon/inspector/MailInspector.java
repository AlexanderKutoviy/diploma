package attendance.daemon.inspector;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public interface MailInspector {
    void fetch() throws Exception;

    void setCpds(ComboPooledDataSource cpds);
}