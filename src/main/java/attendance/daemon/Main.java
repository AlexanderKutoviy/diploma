package attendance.daemon;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.io.IoBuilder;
import tit.utils.ConfigMap;
import titanium.mail.sender.broker.DaemonBroker;
import titanium.mail.sender.broker.TaskBroker;
import titanium.mail.sender.modules.MainModule;
import titanium.mail.sender.worker.MailSenderWorker;

import javax.inject.Singleton;
import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class Main {

    private static Logger l;

    public static void main(String[] args) throws Exception {
        System.setProperty("log4j.shutdownCallbackRegistry", "com.djdch.log4j.StaticShutdownCallbackRegistry");
        Configurator.initialize("Log4j2Conf", "log4j2.xml");
        l = LogManager.getLogger(Main.class);
        System.setProperty("jsse.enableSNIExtension", "false");
        Map<Thread, MailSenderWorker> workers = new HashMap<>();
//        Security.addProvider(new BouncyCastleProvider());

        PrintStream oldErr = System.err;
        PrintStream oldOut = System.out;
        System.setErr(new PrintStream(IoBuilder.forLogger(l).filter(oldErr).buildOutputStream(), true));
        System.setOut(new PrintStream(IoBuilder.forLogger(l).filter(oldOut).buildOutputStream(), true));
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kiev"));
        Locale.setDefault(Locale.US);
        ConfigMap config = new ConfigMap();

        if (new File("config.properties").exists()) {
            config.putAll(ConfigMap.fromPropertiesFile("config.properties"));
        }
        if (new File("config.local.properties").exists()) {
            config.putAll(ConfigMap.fromPropertiesFile("config.local.properties"));
        }

        MainModule mainModule = new MainModule(config);
        Component mainComponent = DaggerMain_Component.builder()
            .mainModule(mainModule)
            .build();

        mainComponent.taskBroker().start();
        mainComponent.daemonBroker().start();

        for (int i = 0; i < config.getInteger("worker.count", 2); ++i) {
            MailSenderWorker worker = mainComponent.worker();
            Thread t = new Thread("MailSenderWorker") {
                @Override
                public void run() {
                    worker.start();
                }
            };
//            t.setDaemon(true);
            t.start();
            workers.put(t, worker);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                l.trace("Shutdown all");
                mainComponent.taskBroker().stopWorking();
                mainComponent.daemonBroker().stop();

                for (Map.Entry<Thread, MailSenderWorker> worker : workers.entrySet()) {
                    worker.getValue().stop();
                }
                if (LogManager.getContext() instanceof LoggerContext) {
                    l.info("Shutting down log4j2");
                    Configurator.shutdown((LoggerContext) LogManager.getContext());
                } else l.warn("Unable recipients shutdown log4j2");
            }
        });
    }

    @Singleton
    @dagger.Component(modules = MainModule.class)
    public interface Component {
        MailSenderWorker worker();
        TaskBroker taskBroker();
        DaemonBroker daemonBroker();
        ComboPooledDataSource cpds();
    }
}