package org.babich.crawler.metrics;

import io.micrometer.core.instrument.Clock;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.babich.crawler.metrics.Utils.*;

/**
 * <p>This class provides a meter register that puts all the metrics in <b>influxdb</b>. You can use a properties file
 * to configure the database connection and registry settings. The file path can be passed to the class constructor,
 * or a default value can be used for the local database instance.</p>
 * <p>
 * <br/>The configuration via the crawler yml file looks like this:
 * <pre>{@code
 *  metrics: &Metrics
 *      registry: !!org.babich.crawler.metrics.InfluxRegistry { propertyFile : './web-crawler-lib/src/main/resources/influx.properties' }
 * }</pre>
 * <p>
 * The property file {@code influx.properties} looks like this:
 *
 * <pre>
 *
 *      influx.db=crawler
 *      influx.uri=http://localhost:8086
 *      influx.step=1
 * </pre>
 */
public class InfluxRegistry extends InfluxMeterRegistry {

    private final static Logger logger = LoggerFactory.getLogger(InfluxRegistry.class);

    private final static InfluxConfig disabledInfluxConfig = new InfluxConfig() {

        @Override
        public String get(String key) {
            return null;
        }

        @Override
        public boolean enabled() {
            return false;
        }
    };


    InfluxRegistry() {
        this(disabledInfluxConfig, Clock.SYSTEM);
    }

    public InfluxRegistry(InfluxConfig config, Clock clock) {
        super(config, clock);
    }

    public InfluxRegistry(String propertyFile) {
        this(new InfluxConfig() {
            final Map<String,String> properties = toImmutableMap(loadProperties(propertyFile));

            {
                logger.info("Influxdb metrics properties: {} ", properties.toString());
            }

            @Override
            public String get(String key) {
                return properties.get(key);
            }

            @Override
            public Duration step() {
                return Optional.ofNullable(properties.get(prefix() + '.' + "step"))
                        .map(value -> Duration.ofSeconds(Long.parseLong(value)))
                        .orElse(Duration.ofSeconds(1));
            }
        }, Clock.SYSTEM);
    }

}
