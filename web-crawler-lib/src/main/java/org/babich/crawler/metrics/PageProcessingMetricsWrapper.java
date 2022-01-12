/*
 * @author Vadim Babich
 */
package org.babich.crawler.metrics;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessing;
import org.babich.crawler.api.processing.AssignedPageProcessing;

/**
 * This class collects a metric for page processing time.
 * The "class" tag is used to identify a particular type of page handler.
 */
public class PageProcessingMetricsWrapper implements AssignedPageProcessing {

    private static final String DEFAULT_METRIC_NAME = "crawler.processing.duration";

    private final AssignedPageProcessing delegate;
    private final Timer timerProcess;


    public static PageProcessingMetricsWrapper of(PageProcessing delegate){
        return new PageProcessingMetricsWrapper(new DefaultPageProcessingPredicate(){
            @Override
            public Iterable<Page> process(Page page) {
                return delegate.process(page);
            }
        }, DEFAULT_METRIC_NAME, "class", Utils.getClassName(delegate.getClass()));
    }

    public PageProcessingMetricsWrapper(AssignedPageProcessing delegate) {
        this(delegate, DEFAULT_METRIC_NAME, "class", Utils.getClassName(delegate.getClass()));
    }

    public PageProcessingMetricsWrapper(AssignedPageProcessing delegate, String metricName, String... tags) {
        this.delegate = delegate;
        this.timerProcess = Metrics.timer(metricName, tags);
    }

    @Override
    public Iterable<Page> process(Page page) {
        return timerProcess.record(() -> delegate.process(page));
    }

    @Override
    public boolean matches(Page page) {
        return delegate.test(page);
    }

    static abstract class DefaultPageProcessingPredicate implements AssignedPageProcessing {

        @Override
        public boolean matches(Page page) {
            return false;
        }
    }
}
