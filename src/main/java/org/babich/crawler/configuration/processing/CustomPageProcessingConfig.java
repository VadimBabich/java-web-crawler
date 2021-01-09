/*
 * @author Vadim Babich
 */
package org.babich.crawler.configuration.processing;

import com.google.common.base.Verify;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessing;
import org.babich.crawler.api.ProcessingMessage;
import org.babich.crawler.configuration.processing.CustomProcessingFilter.When;

/**
 * Used as a code-base page processing configuration container.
 * The {@link CustomPageProcessingConfig#predicate} if it matches the page then the assigned
 * {@link CustomPageProcessingConfig#pageProcessing} and
 * {@link CustomPageProcessingConfig#filter} are applied.
 */
public class CustomPageProcessingConfig {

    private final Function<Page, ProcessingMessage> messageProducer;

    private final Predicate<Page> predicate;

    private final PageProcessing pageProcessing;

    private final CustomProcessingFilter filter;

    private CustomPageProcessingConfig(Function<Page, ProcessingMessage> messageProducer
            , Predicate<Page> predicate
            , PageProcessing pageProcessing
            , CustomProcessingFilter filter) {
        this.messageProducer = messageProducer;
        this.predicate = predicate;
        this.pageProcessing = pageProcessing;
        this.filter = filter;
    }

    public Function<Page, ProcessingMessage> getMessageProducer() {
        return messageProducer;
    }

    public Predicate<Page> getPredicate() {
        return predicate;
    }

    public PageProcessing getPageProcessing() {
        return pageProcessing;
    }

    public CustomProcessingFilter getFilter() {
        return filter;
    }


    public static class Builder {
        Function<Page, ProcessingMessage> messageProducer;
        Predicate<Page> predicate;
        PageProcessing pageProcessing;
        CustomProcessingFilter filter;

        /**
         * @param messageProducer a factory that produces a message object for the page being processed.
         */
        public Builder sendMessage(
                Function<Page, ProcessingMessage> messageProducer) {
            this.messageProducer = messageProducer;
            return this;
        }

        /**
         * @param predicate page matcher that assigns processing and filters to the page
         */
        public Builder forPages(Predicate<Page> predicate) {
            this.predicate = predicate;
            return this;
        }

        /**
         * @param pageProcessing processing service assigned to to the specified page using the
         *                       {@link Builder#forPages(java.util.function.Predicate)} method.
         */
        public Builder processingBy(PageProcessing pageProcessing) {
            this.pageProcessing = pageProcessing;
            return this;
        }

        /**
         * Excludes the page assigned to be processed by
         * the {@link Builder#forPages(java.util.function.Predicate)} method.
         * <br/>The skipped page will not be processed by other page processing services.
         * @param when builder of exclude filter
         */
        public Builder skippingPages(Consumer<When> when) {
            When builder = new When();
            when.accept(builder);
            this.filter = builder.build();
            return this;
        }

        public CustomPageProcessingConfig build(){
            Verify.verifyNotNull(predicate, "predicate cannot be null.");

            return new CustomPageProcessingConfig(messageProducer, predicate, pageProcessing,  filter);
        }
    }

    public static class Matchers{

        private Matchers() {
        }

        public static Predicate<Page> regexpPageUrlMatcher(String regExp) {
            return page -> Pattern.compile(regExp)
                    .matcher(page.getPageUrl())
                    .matches();
        }

        public static Predicate<Page> hostNameMatcher(String hostName){
            return page -> {
                try {
                    return hostName.equalsIgnoreCase(new URL(page.getPageUrl()).getHost());
                } catch (MalformedURLException e) {
                    return false;
                }
            };
        }

        public static Predicate<Page> alwaysTrue(){
            return page -> true;
        }

    }
}
