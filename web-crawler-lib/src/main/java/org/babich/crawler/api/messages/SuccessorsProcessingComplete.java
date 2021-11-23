/*
 * @author Vadim Babich
 */
package org.babich.crawler.api.messages;

import java.util.StringJoiner;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.ProcessingMessage;


/**
 * Message - when every link of {@code page} has been processed.
 */
public class SuccessorsProcessingComplete extends ProcessingMessage {

    private static final long serialVersionUID = 1;

    private final Page page;

    public SuccessorsProcessingComplete(Page page) {
        this.page = page;
    }

    public Page getPage() {
        return page;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SuccessorsProcessingComplete.class.getSimpleName() + "[", "]")
                .add("time=" + time)
                .add("page=" + page)
                .toString();
    }
}
