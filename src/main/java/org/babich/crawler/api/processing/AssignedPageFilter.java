/*
 * @author Vadim Babich
 */
package org.babich.crawler.api.processing;

import java.util.function.Function;
import java.util.function.Predicate;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.messages.PageProcessingSkippe;

public interface AssignedPageFilter extends Predicate<Page>, Function<Page, PageProcessingSkippe> {

    default boolean test(Page page) {
        return matches(page);
    }

    boolean matches(Page page);


    static AssignedPageFilter of(Predicate<Page> relevantPredicate
            , Predicate<Page> filter
            , Function<Page, PageProcessingSkippe> pagePreProcessing) {

        final Function<Page, PageProcessingSkippe> message = null == pagePreProcessing
                ? page -> new PageProcessingSkippe(page, "Custom page processing filter.")
                : pagePreProcessing;

        return new AssignedPageFilter() {

            @Override
            public PageProcessingSkippe apply(Page page) {
                return message.apply(page);
            }

            @Override
            public boolean matches(Page page) {
                return relevantPredicate.and(filter).test(page);
            }
        };
    }
}
