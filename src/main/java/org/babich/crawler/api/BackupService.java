/*
 * @author Vadim Babich
 */
package org.babich.crawler.api;

import java.util.Collection;

/**
 * The service provides an approach to backup and restore the state of the crawler in case of failure.
 */
public interface BackupService {

    /**
     *This method restores the previous state of the crawler session.
     *It should send {@code PageRecovered} message for every page that processed and found in the previous session.
     */
    void backup();

    /**
     * This method restores the previous crawler session state
     * @param pageContext the page context that is used to restore pages from the backup.
     * @return a set of pages that are used as start pages for the Crawler
     */
    Collection<Page> restoreFor(PageContext pageContext);
}
