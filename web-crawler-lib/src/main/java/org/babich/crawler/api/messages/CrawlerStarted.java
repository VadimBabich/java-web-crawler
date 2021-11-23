/*
 * @author Vadim Babich
 */
package org.babich.crawler.api.messages;

import java.io.Serializable;
import java.util.StringJoiner;
import org.babich.crawler.api.ProcessingMessage;


/**
 * The message is triggered before the page starts processing.
 * The {@code payload} of this message may contain configuration information.
 */
public class CrawlerStarted extends ProcessingMessage {

    private static final long serialVersionUID = 1;

    private final Serializable payload;

    public CrawlerStarted(Serializable payload) {
        this.payload = payload;
    }

    public Serializable getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CrawlerStarted.class.getSimpleName() + "[", "]")
                .add("time=" + time)
                .add("payload=" + payload)
                .toString();
    }
}
