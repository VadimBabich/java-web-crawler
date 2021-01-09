/*
 * @author Vadim Babich
 */
package org.babich.crawler.api;

/**
 * Page processing service interface that provides a contract for downloads and parses resources.
 */
public interface PageProcessing {

    /**
     * Download and parse {@code page}
     * @param page contains the URL to download the resource
     * @return collection of successor found when processing this {@code page}.
     */
    Iterable<Page> process(Page page);
}
