package attendance.daemon.broker;


import titanium.mail.sender.dao.Provider;
import titanium.mail.sender.provider.MailProvider;

import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

public class ProviderHolder {

    public final Provider providerModel;

    public MailProvider provider;

    public Date lastModified;

    public final ArrayBlockingQueue<TaskSet> tasks = new ArrayBlockingQueue<>(20);

    public ProviderHolder.State state = ProviderHolder.State.RUNNING;

    public ProviderHolder newHolder;

    public enum State {
        RUNNING(),
        STOPPING()
    }

    public ProviderHolder(Provider providerModel) {
        this.providerModel = providerModel;
    }
}
