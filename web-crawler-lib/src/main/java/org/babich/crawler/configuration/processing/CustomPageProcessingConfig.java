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
import org.babich.crawler.configuration.ApplicationConfig;
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

        private final ApplicationConfig applicationConfig;

        public Builder(ApplicationConfig applicationConfig) {
            this.applicationConfig = applicationConfig;
        }

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

        public ApplicationConfig getApplicationConfig() {
            return applicationConfig;
        }
    }

    /**
     * The matching tool, which is used in the page processing configuration,
     * helps to categorize all found pages according to specific processing and message types.
     */
    public static class Matchers{

        private Matchers() {
        }

        /**
         * Matching the entire page url to the provided regex
         * @param regExp regular expression as string
         * @return page predicate
         */
        public static Predicate<Page> regexpPageUrlMatcher(String regExp) {
            Pattern pattern = Pattern.compile(regExp);
            return page -> pattern.matcher(page.getPageUrl())
                    .matches();
        }

        /**
         * matching the hostname of the page url with the provided hostname regex
         * @param hostName hostname regex for example: .*host.com
         * @return page predicate
         */
        public static Predicate<Page> hostNameMatcher(String hostName){
            return page -> {
                try {
                    return new URL(page.getPageUrl()).getHost().matches(hostName);
                } catch (MalformedURLException e) {
                    return false;
                }
            };
        }

        /**
         * always true predicate for any page
         * @return page predicate
         */
        public static Predicate<Page> alwaysTrue(){
            return page -> true;
        }

    }
}
