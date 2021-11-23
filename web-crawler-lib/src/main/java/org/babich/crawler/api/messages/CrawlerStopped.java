/*
 * @author Vadim Babich
 */
package org.babich.crawler.api.messages;

import java.io.Serializable;
import java.util.StringJoiner;
import org.babich.crawler.api.ProcessingMessage;


/**
 * This message is sent when all pages have been processed.
 * The message payload can contain any generalized information at the end of the process.
 */
public class CrawlerStopped extends ProcessingMessage {

    private static final long serialVersionUID = 1;

    private final Serializable payload;

    //true if the processing ended with an error
    private final boolean abnormal;

    public CrawlerStopped(Serializable payload) {
        this(payload, false);
    }

    public CrawlerStopped(Serializable payload, boolean abnormal) {
        this.payload = payload;
        this.abnormal = abnormal;
    }

    public Serializable getPayload() {
        return payload;
    }

    public boolean isAbnormal() {
        return abnormal;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CrawlerStopped.class.getSimpleName() + "[", "]")
                .add("payload=" + payload)
                .add("abnormal=" + abnormal)
                .add("time=" + time)
                .toString();
    }
}
