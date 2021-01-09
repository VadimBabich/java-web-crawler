/*
 * @author Vadim Babich
 */
package org.babich.crawler.api.messages;

import java.util.StringJoiner;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.ProcessingMessage;


/**
 * This event occurs when a {@code page} skips processing for some reason (often filtering). Message contains {@code
 * time} and skipped {@code page}
 */
public class PageProcessingSkippe extends ProcessingMessage {

    private static final long serialVersionUID = 1;

    private final Page page;

    //the cause why page processing was skipped
    private final String cause;

    public PageProcessingSkippe(Page page) {
        this(page, null);
    }

    public PageProcessingSkippe(Page page, String cause) {
        this.page = page;
        this.cause = cause;
    }

    public Page getPage() {
        return page;
    }

    public String getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PageProcessingSkippe.class.getSimpleName() + "[", "]")
                .add("page=" + page)
                .add("cause='" + cause + "'")
                .toString();
    }
}
