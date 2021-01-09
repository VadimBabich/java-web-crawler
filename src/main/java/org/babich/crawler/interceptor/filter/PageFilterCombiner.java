/*
 * @author Vadim Babich
 */
package org.babich.crawler.interceptor.filter;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessingInterceptor;
import org.babich.crawler.api.processing.AssignedPageFilter;
import org.babich.crawler.configuration.exception.PreProcessingChainException;
import org.babich.crawler.event.LocalEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageFilterCombiner implements PageProcessingInterceptor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private LocalEventBus eventBus;

    private Set<AssignedPageFilter> assignedPageFilters;

    private PageFilterCombiner() {
    }

    public PageFilterCombiner(LocalEventBus eventBus, Set<AssignedPageFilter> customPageFilters) {
        this.eventBus = eventBus;

        this.assignedPageFilters = Optional.ofNullable(customPageFilters)
                .orElse(Collections.emptySet());
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE + 100;
    }

    @Override
    public void beforeProcessing(Page page) {

        Optional<AssignedPageFilter> triggeredFilter = assignedPageFilters.stream()
                .filter(preProcessing -> preProcessing.matches(page))
                .findFirst();

        if (!triggeredFilter.isPresent()) {
            return;
        }

        Predicate<Page> filter = triggeredFilter.get();

        Optional.ofNullable(eventBus)
                .ifPresent(bus -> bus.post(triggeredFilter.get().apply(page)));

        throw new PreProcessingChainException(String
                .format("Page {%s} processing skipped due to custom filter {%s}.", page.getPageName(), filter));
    }
}
