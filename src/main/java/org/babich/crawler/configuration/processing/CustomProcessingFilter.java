/*
 * @author Vadim Babich
 */
package org.babich.crawler.configuration.processing;

import java.util.function.Function;
import java.util.function.Predicate;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.messages.PageProcessingSkippe;

/**
 * Used as a code-base page filter configuration container.
 */
public class CustomProcessingFilter {

    private final Predicate<Page> predicate;

    private final Function<Page, PageProcessingSkippe> messageFactory;

    private CustomProcessingFilter(Predicate<Page> predicate, Function<Page, PageProcessingSkippe> messageFactory) {
        this.predicate = predicate;
        this.messageFactory = messageFactory;
    }

    public Predicate<Page> getPredicate() {
        return predicate;
    }

    public Function<Page, PageProcessingSkippe> getMessageFactory() {
        return messageFactory;
    }

    public static class When {

        private Predicate<Page> filter;
        private Function<Page, PageProcessingSkippe> messageFactory;


        /**
         * @param excludes the page predicate used as a filter. If the predicate is true,
         *                 the page should be skipped for processing.
         */
        public When conditionIsTrue(Predicate<Page> excludes) {
            this.filter = excludes;
            return this;
        }

        /**
         * @param messageFactory the factory that produces a message object when the filter
         *                       is triggered (predicate is true)
         */
        public When andSendMessage(
                Function<Page, PageProcessingSkippe> messageFactory) {
            this.messageFactory = messageFactory;
            return this;
        }

        public CustomProcessingFilter build(){
            if (null == filter) {
                throw new IllegalArgumentException("filter cannot be null");
            }

            return new CustomProcessingFilter(filter, messageFactory);
        }
    }
}
