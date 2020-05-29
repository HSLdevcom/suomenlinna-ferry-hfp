package fi.hsl.suomenlinna_hfp.common.utils;

import java.util.concurrent.ThreadFactory;

public class DaemonThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable runnable) {
        final Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    }
}
