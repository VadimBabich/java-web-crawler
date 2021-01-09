/*
 * @author Vadim Babich
 */
package org.babich.crawler.api;

/**
 * {@code Order} defines the sort order for an implemented component. Lower values have higher priority.
 * Ordering is supported for preprocessing and postprocessing interceptors.
 */
public interface Order {

    default int getOrder() {
        return 0;
    }
}
