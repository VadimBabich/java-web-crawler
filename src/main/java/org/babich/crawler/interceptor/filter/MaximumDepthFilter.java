/*
 * @author Vadim Babich
 */
package org.babich.crawler.interceptor.filter;

import java.util.Optional;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessingInterceptor;
import org.babich.crawler.api.messages.PageProcessingSkippe;
import org.babich.crawler.configuration.ApplicationConfig.Limit;
import org.babich.crawler.configuration.exception.PreProcessingChainException;
import org.babich.crawler.event.LocalEventBus;

/**
 * Maximum depth filter so removing pages with a depth more than a threshold value obtained from {@code Limit#maxDepth}.
 * <br/>The {@code Limit#maxDepth} value can be configured through the {@code WebCrawler.WebCrawlerBuilder}
 * or the {@code crawler.yml} configuration file.
 */
public class MaximumDepthFilter implements PageProcessingInterceptor {

    private final Limit limit;
    private final LocalEventBus eventBus;

    public MaximumDepthFilter() {
        this(null, null);
    }

    public MaximumDepthFilter(Limit limit, LocalEventBus eventBus) {
        this.limit = limit;
        this.eventBus = eventBus;
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void beforeProcessing(Page page) {
        int maxDepth;
        if (0 > (maxDepth = getDepth(limit))) {
            return;
        }

        if (page.getDepth() <= maxDepth) {
            return;
        }

        String message = String
                .format("The page {%s} by url {%s} has been filtered by depth {%d}. Maximum allowable depth {%d}"
                        , page.getPageName(), page.getPageUrl(), page.getDepth(), maxDepth);

        Optional.ofNullable(eventBus)
                .ifPresent(bus -> bus.post(new PageProcessingSkippe(new Page(page), message)));

        throw new PreProcessingChainException(message);
    }

    private int getDepth(Limit limit){
        return null == limit ? -1 : limit.getMaxDepth();
    }
}
