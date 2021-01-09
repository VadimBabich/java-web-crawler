/*
 * @author Vadim Babich
 */
package org.babich.crawler.event;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.MoreExecutors;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("UnstableApiUsage")
public class LocalEventBus {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public enum Mode {
        ASYNC,
        SYNC;
    }

    private final EventBus delegate;

    private final Mode mode;

    public LocalEventBus() {
        this("SYNC");
    }

    public LocalEventBus(String mode) {
        this(mode, null);
    }

    public LocalEventBus(String mode, ExecutorService executor) {
        this.mode = Mode.valueOf(mode);

        if (null == executor) {
            executor = this.mode == Mode.ASYNC ? new ForkJoinPool() : MoreExecutors.newDirectExecutorService();
        }

        setupShutdownHook(executor);

        this.delegate = this.mode == Mode.ASYNC
                ? new AsyncEventBus(executor, new EventBusExceptionHandler())
                : new EventBus(new EventBusExceptionHandler());
    }

    public Mode getMode() {
        return mode;
    }

    public void register(Object object) {
        delegate.register(object);
    }


    public void unregister(Object object) {
        delegate.unregister(object);
    }


    public void post(Object event) {
        delegate.post(event);
    }

    private void setupShutdownHook(ExecutorService executor){
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            MoreExecutors.shutdownAndAwaitTermination(executor, Duration.ofMinutes(2));
            logger.debug("EventBus has been stopped");
        }));
    }
}
