/*
 * @author Vadim Babich
 */
package org.babich.crawler.metrics;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessingInterceptor;

import java.util.List;

import static org.babich.crawler.metrics.Utils.getClassName;

/**
 * This class implements time metrics for crawler services (which are implemented as page processing interceptors).
 * Each service could implement the "beforeProcessing" and "afterProcessing" methods,
 * the "method" tag is used to identify specific method.
 */
public class PageProcessingInterceptorMetricsWrapper implements PageProcessingInterceptor {

    private final Timer timerBeforeProcessing;
    private final Timer timerAfterProcessing;
    private final PageProcessingInterceptor delegate;


    public PageProcessingInterceptorMetricsWrapper(PageProcessingInterceptor delegate) {
        this(delegate, "crawler.processing.service.duration", getClassName(delegate.getClass()));
    }

    public PageProcessingInterceptorMetricsWrapper(PageProcessingInterceptor delegate, String metricName, String tag) {
        this.delegate = delegate;
        this.timerBeforeProcessing = Metrics.timer(metricName, "class", tag, "method", "beforeProcessing");
        this.timerAfterProcessing = Metrics.timer(metricName, "class", tag, "method", "afterProcessing");
    }

    @Override
    public void beforeProcessing(Page page) {
        timerBeforeProcessing.record(() -> delegate.beforeProcessing(page));
    }

    @Override
    public void afterProcessing(Page page, List<Page> successorPages) {
        timerAfterProcessing.record(() -> delegate.afterProcessing(page, successorPages));
    }

}
