/*
 * @author Vadim Babich
 */
package org.babich.crawler.api.messages;

import java.util.StringJoiner;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.ProcessingMessage;


/**
 * This message appears before {@code page} processing. This class contains {@code time} and a {@code page} for which
 * all related pages were loaded.
 */
public class PageProcessingStart extends ProcessingMessage {

    private static final long serialVersionUID = 1;

    private final Page page;

    public PageProcessingStart(Page page) {
        this.page = page;
    }

    public Page getPage() {
        return page;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PageProcessingStart.class.getSimpleName() + "[", "]")
                .add("time=" + time)
                .add("page=" + page)
                .toString();
    }
}
