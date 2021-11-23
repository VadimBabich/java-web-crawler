/*
 * @author Vadim Babich
 */
package org.babich.crawler.interceptor.filter;

import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessingInterceptor;
import org.babich.crawler.api.messages.PageProcessingSkippe;
import org.babich.crawler.api.messages.PageRecovered;
import org.babich.crawler.configuration.exception.PreProcessingChainException;
import org.babich.crawler.event.LocalEventBus;

/**
 * This filter detects the circular links and skips their pages.
 */
@SuppressWarnings("UnstableApiUsage")
public class CircularLinksFilter implements PageProcessingInterceptor {

    private final Set<String> processedLinks = Sets.newConcurrentHashSet();

    private LocalEventBus eventBus;

    private CircularLinksFilter() {
    }

    public CircularLinksFilter(LocalEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE + 100;
    }

    @Override
    public void beforeProcessing(Page page) {
        if (!processedLinks.contains(page.getPageUrl())) {
            return;
        }

        if (page.getPageContextRef().get().getPagesProcessed() == 1) {
            return;
        }

        String message = String.format("The page {%s} by url {%s} has already been processed."
                        , page.getPageName(), page.getPageUrl());

        Optional.ofNullable(eventBus)
                .ifPresent(bus -> bus.post(new PageProcessingSkippe(new Page(page), message)));

        throw new PreProcessingChainException(message);
    }

    @Override
    public void afterProcessing(Page page, List<Page> successorPages) {
        processedLinks.add(page.getPageUrl());
    }

    @Subscribe
    public void pageOnRecovered(PageRecovered message) {
        if (message.isProcessed()) {
            processedLinks.add(message.getPage().getPageUrl());
        }
    }
}
