/*
 * @author Vadim Babich
 */
package org.babich.crawler.metrics;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.babich.crawler.event.LocalEventBus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * <p>This class extends the {@code LocalEventBus} class and implements metrics of timer a message dispatch.
 * To identify a specific type of message, the "class" tag is used.</p>
 *
 * <br/>The configuration via the crawler yml file looks like this:
 * <pre>{@code
 *     eventBus: &EventBus
 *         !!org.babich.crawler.metrics.EventBusWithMetricsExtension { mode : 'ASYNC' } *
 * }</pre>
 */
public class EventBusWithMetricsExtension extends LocalEventBus {

    private final Map<String, Timer> counters = new ConcurrentHashMap<>();

    public EventBusWithMetricsExtension() {
    }

    public EventBusWithMetricsExtension(String mode) {
        super(mode);
    }

    public EventBusWithMetricsExtension(String mode, ExecutorService executor) {
        super(mode, executor);
    }


    @Override
    public void post(Object event) {
        getCounterBy(event).record(() -> super.post(event));
    }

    private Timer getCounterBy(Object event) {
        String eventClassName = Utils.getClassName(event.getClass());
        Function<String, Timer> eventClassNameToCounter = key ->
                Metrics.timer("crawler.eventbus.messages.count", "class", eventClassName);

        return counters.computeIfAbsent(eventClassName, eventClassNameToCounter);
    }
}
