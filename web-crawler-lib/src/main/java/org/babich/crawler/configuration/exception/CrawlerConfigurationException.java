/*
 * @author Vadim Babich
 */
package org.babich.crawler.configuration.exception;

/**
 * This exception is thrown when there is a crawler configuration problem.
 */
public class CrawlerConfigurationException extends Exception{

    public CrawlerConfigurationException(String message) {
        super(message);
    }

    public CrawlerConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
