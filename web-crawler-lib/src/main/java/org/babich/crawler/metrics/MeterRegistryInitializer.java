/*
 * @author Vadim Babich
 */
package org.babich.crawler.metrics;

import com.google.common.eventbus.Subscribe;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.babich.crawler.api.messages.CrawlerStarted;
import org.babich.crawler.api.messages.CrawlerStopped;

import java.util.Optional;

import static org.babich.crawler.metrics.Utils.bindJVMMetrics;


/**
 * <p>Since many scanners can be run on the same JVM, each of them requires a separate initialization and closing
 * of the {@code MeterRegistry}. Initialization occurs on an event with the message {@code CrawlerStarted},
 * and the MeterRegistry is closed on an event with the message {@code CrawlerStopped}.</p>
 *
 * <br/>The configuration via the crawler yml file looks like this:
 * <pre>{@code
 *     metrics: &Metrics
 *          registry: !!org.babich.crawler.metrics.InfluxRegistry { propertyFile : './web-crawler-lib/src/main/resources/influx.properties' }
 *
 *     interceptorList:
 *       - *BackupService
 *       ...
 *       - !!org.babich.crawler.metrics.PageMetricsInterceptor { config: *Metrics }
 * }</pre>
 */
public class MeterRegistryInitializer {

    private final MeterRegistry registry;


    MeterRegistryInitializer() {
        this.registry = null;
    }

    public MeterRegistryInitializer(org.babich.crawler.configuration.ApplicationConfig.Metrics config) {
        this.registry = getRegistry(config);
    }

    @Subscribe
    public void onStart(CrawlerStarted message) {
        Optional.ofNullable(registry).ifPresent(item -> initMetrics((String) message.getPayload()));
    }

    @Subscribe
    public void onStop(CrawlerStopped message){
        Optional.ofNullable(registry).ifPresent(MeterRegistry::close);
    }

    private void initMetrics(String name){
        registry.config().commonTags("crawler.name", name);
        Metrics.addRegistry(bindJVMMetrics(registry));
    }

    MeterRegistry getRegistry(org.babich.crawler.configuration.ApplicationConfig.Metrics config){
        if(!config.getEnabled()){
            return null;
        }
        return Optional.ofNullable(config.getRegistry()).orElse(new SimpleMeterRegistry());
    }
}
