package attendance.daemon.modules;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import dagger.Module;
import dagger.Provides;
import tit.utils.ConfigMap;
import titanium.mail.sender.broker.DaemonBroker;
import titanium.mail.sender.broker.TaskBroker;

import javax.inject.Singleton;
import java.util.Properties;

@Module
public class MainModule {

    private ConfigMap config;

    public MainModule(ConfigMap config) {
        this.config = config;
    }

    @Provides
    @Singleton
    public TaskBroker broker(ComboPooledDataSource cpds) {
        return new TaskBroker(cpds);
    }

    @Provides
    @Singleton
    public DaemonBroker daemonBroker(ComboPooledDataSource cpds) {
        return new DaemonBroker(cpds);
    }

    @Provides
    @Singleton
    public ComboPooledDataSource cpds() {
        try {
            ComboPooledDataSource cpds = new ComboPooledDataSource();
            new com.mysql.jdbc.Driver();
            cpds.setDriverClass("com.mysql.jdbc.Driver");
            Properties properties = new Properties();
            properties.setProperty("characterEncoding", "UTF-8");
            properties.setProperty("user", config.get("db.user", ""));
            properties.setProperty("password", config.get("db.password", ""));

            cpds.setJdbcUrl(config.get("db.dsn", ""));
            cpds.setProperties(properties);
            cpds.setInitialPoolSize(5);
            cpds.setMinPoolSize(5);
            cpds.setAcquireIncrement(5);
            cpds.setMaxPoolSize(20);
            cpds.setMaxStatements(100);
            cpds.setAcquireRetryAttempts(30);
            cpds.setAutoCommitOnClose(false);
            cpds.setPreferredTestQuery("SELECT 1");
            cpds.setTestConnectionOnCheckin(false);
            cpds.setTestConnectionOnCheckout(true);
            cpds.setIdleConnectionTestPeriod(60);
            cpds.setMaxConnectionAge(300000);
            cpds.setMaxIdleTime(10800);
            cpds.setUnreturnedConnectionTimeout(300000);
            return cpds;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
