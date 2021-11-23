/*
 * @author Vadim Babich
 */
package org.babich.crawler.api;

import java.util.List;

/**
 * Interceptor that should invoke around the pageProcessing object.
 */
public interface PageProcessingInterceptor extends Order {

    /**
     * called before invoking the method {@link PageProcessing#process(org.babich.crawler.api.Page)}
     * @param page currently processed page
     */
    default void beforeProcessing(Page page){
    }

    /**
     * called after invoking the method {@link PageProcessing#process(org.babich.crawler.api.Page)}
     * @param page currently processed page
     * @param successorPages found when processing this page
     */
    default void afterProcessing(Page page, List<Page> successorPages) {
    }

}
