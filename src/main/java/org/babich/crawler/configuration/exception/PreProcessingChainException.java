/*
 * @author Vadim Babich
 */
package org.babich.crawler.configuration.exception;

/**
 * Used when it is necessary to prevent execution of processing for the current page.
 * No child links will be returned for this page.
 */
public class PreProcessingChainException extends RuntimeException {

    /**
     * The message should indicate the reason why the page stops being processed.
     * @param message reason
     */
    public PreProcessingChainException(String message) {
        super(message);
    }
}
