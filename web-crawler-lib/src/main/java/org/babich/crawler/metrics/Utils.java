package org.babich.crawler.metrics;

import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class Utils {

    /**
     * This function returns the class name without the package name. This also works for enclosing classes.
     * @param klass Class of object
     * @return simple class name
     */
    public static String getClassName(Class<?> klass){
        return Optional.ofNullable(klass.getEnclosingClass())
                .map(Class::getSimpleName)
                .orElse(klass.getSimpleName());
    }

    public static MeterRegistry bindJVMMetrics(MeterRegistry registry){
        new JvmThreadMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);
        return registry;
    }

    public static Properties loadProperties(String propertyFile){
        Properties properties = new Properties();
        try(InputStream inputStream = new FileInputStream(propertyFile)){
            properties.load(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties;
    }

    public static Map<String, String> toImmutableMap(Properties properties){
        //noinspection unchecked
        return ImmutableMap.copyOf((Map)properties);
    }
}
