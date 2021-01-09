/*
 * @author Vadim Babich
 */
package org.babich.crawler.api.messages;

import java.util.StringJoiner;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.ProcessingMessage;


/**
 * This message appears when a {@code page} has been successfully processed.
 * Message contains a processed {@code page}.
 */
public class PageProcessingComplete extends ProcessingMessage {
    private static final long serialVersionUID = 1;

    private final Page page;

    public PageProcessingComplete(Page page) {
        this.page = page;
    }

    public Page getPage() {
        return page;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PageProcessingComplete.class.getSimpleName() + "[", "]")
                .add("time=" + time)
                .add("page=" + page)
                .toString();
    }
}
