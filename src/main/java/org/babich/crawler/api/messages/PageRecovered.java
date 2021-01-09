/*
 * @author Vadim Babich
 */
package org.babich.crawler.api.messages;

import java.util.StringJoiner;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.ProcessingMessage;

/**
 * message - when a page was recovered from the backup.
 */
public class PageRecovered extends ProcessingMessage {

    private static final long serialVersionUID = 1;

    //this flag is true if the page was processed in a previous session.
    private final boolean processed;

    //recovered page
    private final Page page;

    public PageRecovered(Page page) {
        this(page, false);
    }

    public PageRecovered(Page page, boolean processed) {
        this.processed = processed;
        this.page = page;
    }

    public Page getPage() {
        return page;
    }

    public boolean isProcessed() {
        return processed;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PageRecovered.class.getSimpleName() + "[", "]")
                .add("processed=" + processed)
                .add("page=" + page)
                .add("time=" + time)
                .toString();
    }
}
