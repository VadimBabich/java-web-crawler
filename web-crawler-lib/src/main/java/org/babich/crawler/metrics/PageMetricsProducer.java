/*
 * @author Vadim Babich
 */
package org.babich.crawler.metrics;

import com.google.common.eventbus.Subscribe;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import org.babich.crawler.api.messages.PageProcessingComplete;
import org.babich.crawler.api.messages.PageProcessingSkippe;

/**
 * <p>This class collects a metric for the size of loaded pages.</p>
 *
 * <br/>The configuration via the crawler yml file looks like this:
 * <pre>{@code
 *     interceptorList:
 *       - *BackupService
 *       ...
 *       - !!org.babich.crawler.metrics.PageMetricsProducer { }
 * }</pre>
 */

public class PageMetricsProducer {

    private final DistributionSummary pageSizeSummary;
    private final Counter skippedPageCounter;
    private final Counter completedPageCounter;


    public PageMetricsProducer() {
        this.pageSizeSummary = DistributionSummary
                .builder("crawler.processing.page.size")
                .baseUnit("byte")
                .register(Metrics.globalRegistry);

        this.skippedPageCounter = Metrics.counter("crawler.processing.page.skipped.count");
        this.completedPageCounter = Metrics.counter("crawler.processing.page.completed.count");
    }

    @Subscribe
    public void onSkip(PageProcessingSkippe message){
        skippedPageCounter.increment();
    }

    @Subscribe
    public void OnProcess(PageProcessingComplete message){
        completedPageCounter.increment();
        pageSizeSummary.record(message.getPage().getSize());
    }
}
