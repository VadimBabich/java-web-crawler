/*
 * @author Vadim Babich
 */
package org.babich.crawler.api.processing;

import java.util.function.Function;
import java.util.function.Predicate;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessingInterceptor;
import org.babich.crawler.api.ProcessingMessage;
import org.babich.crawler.api.messages.PageProcessingComplete;

public interface AssignedPagePostProcessing
        extends PageProcessingInterceptor, Predicate<Page>, Function<Page, ProcessingMessage> {

    default boolean test(Page page) {
        return matches(page);
    }

    boolean matches(Page page);


    static AssignedPagePostProcessing of(Predicate<Page> relevantPredicate
            , Function<Page, ProcessingMessage> pagePreProcessing) {

        final Function<Page, ProcessingMessage> message = null == pagePreProcessing
                ? PageProcessingComplete::new
                : pagePreProcessing;

        return new AssignedPagePostProcessing() {

            @Override
            public ProcessingMessage apply(Page page) {
                return message.apply(page);
            }

            @Override
            public boolean matches(Page page) {
                return relevantPredicate.test(page);
            }
        };
    }
}
