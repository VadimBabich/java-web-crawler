/*
 * @author Vadim Babich
 */
package org.babich.crawler.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import org.apache.commons.lang3.StringUtils;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessingInterceptor;

import java.util.List;

/**
 * <p>This class collects a metric for the size of loaded pages.</p>
 *
 * <br/>The configuration via the crawler yml file looks like this:
 * <pre>{@code
 *     interceptorList:
 *       - *BackupService
 *       ...
 *       - !!org.babich.crawler.metrics.PageMetricsInterceptor { }
 * }</pre>
 */
public class PageMetricsInterceptor implements PageProcessingInterceptor {

    private final DistributionSummary pageSizeSummary;


    public PageMetricsInterceptor() {
        this.pageSizeSummary = DistributionSummary
                .builder("crawler.processing.page.size")
                .baseUnit("kb")
                .register(Metrics.globalRegistry);
    }

    @Override
    public void afterProcessing(Page page, List<Page> successorPages) {

        if (StringUtils.isBlank(page.getPageSource())) {
            pageSizeSummary.record(0);
            return;
        }

        pageSizeSummary.record((double)page.getPageSource().length() / 1_024L);
    }

}
