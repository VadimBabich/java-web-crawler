/*
 * @author Vadim Babich
 */
package org.babich.crawler.api;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;
import org.babich.crawler.configuration.ProxyFactory;

/**
 * The context that is passed from one page to another on each load. Contains a processing service and the current
 * state of the process, such as the number of pages processed and all found at the moment.
 * It is a class of an immutable object that can be changed using a {@code transform} method
 * and {@code PageContext.Builder} to create a new one.
 */
public class PageContext implements Serializable {

    private final transient PageProcessing pageProcessing;

    private final int pageCount;

    private final int pagesProcessed;


    private PageContext(PageProcessing pageProcessing, int pageCount, int pagesProcessed) {

        this.pageProcessing = pageProcessing;
        this.pageCount = pageCount;
        this.pagesProcessed = pagesProcessed;
    }

    PageContext(PageContext pageContext){
        this.pageProcessing = null;
        this.pageCount = pageContext.pageCount;
        this.pagesProcessed = pageContext.pagesProcessed;
    }


    /**
     * @return count off all found pages
     */
    public int getPageCount() {
        return pageCount;
    }

    /**
     * @return the number of pages processed
     */
    public PageProcessing getPageProcessing() {
        return pageProcessing;
    }

    /**
     * @return page processing service that downloads and parses resources
     */
    public int getPagesProcessed() {
        return pagesProcessed;
    }

    /**
     * @param builderConsumer use the consumer's builder to change the context.
     * @return new modified context.
     */
    public PageContext transform(Consumer<Builder> builderConsumer) {
        Builder builder = new Builder()
                .pageCount(this.pageCount)
                .pagesProcessed(this.pagesProcessed)
                .pageProcessing(this.pageProcessing);

        builderConsumer.accept(builder);
        return builder.build();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PageContext.class.getSimpleName() + "[", "]")
                .add("processingFlow=" + pageProcessing)
                .add("pageCount=" + pageCount)
                .add("pagePassed=" + pagesProcessed)
                .toString();
    }

    /**
     * Use this builder to create a new {@code PageContext}.
     */
    public static class Builder {

        private List<PageProcessingInterceptor> interceptors;
        private PageProcessing pageProcessing;
        private int pageCount;
        private int pagesProcessed;

        public Builder pageCount(int pageCount) {
            this.pageCount = pageCount;
            return this;
        }


        public Builder pagesProcessed(int pagePassed) {
            this.pagesProcessed = pagePassed;
            return this;
        }

        public Builder pageProcessing(PageProcessing pageProcessing) {
            this.pageProcessing = pageProcessing;
            return this;
        }

        /**
         *
         * @param interceptors of page processing such as filters, services and message producers
         */
        public Builder interceptors(PageProcessingInterceptor... interceptors) {
            this.interceptors = Arrays.asList(interceptors.clone());
            return this;
        }

        public PageContext build() {
            PageProcessing processing = pageProcessingSetUp(this.pageProcessing, this.interceptors);
            return new PageContext(processing, pageCount, pagesProcessed);
        }

        /**
         * setting interceptors on a page processing service
         * @param pageProcessing origin {@code PageProcessing} object.
         * @param interceptors a list of interceptors that should invoke around the pageProcessing object.
         * @return proxied {@code PageProcessing} instance
         */
        private PageProcessing pageProcessingSetUp(PageProcessing pageProcessing,
                List<PageProcessingInterceptor> interceptors) {

            //interceptors are based on Java proxy object, so multiple proxies must be avoided.
            return isProxyRequired(pageProcessing, interceptors)
                    ? ProxyFactory.configureProcessingProxy(pageProcessing, interceptors)
                    : pageProcessing;
        }

        private boolean isProxyRequired(PageProcessing pageProcessing, List<PageProcessingInterceptor> interceptors) {
            return !((null == interceptors || interceptors.isEmpty()) && Proxy.isProxyClass(pageProcessing.getClass()));
        }
    }
}
