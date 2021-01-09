/*
 * @author Vadim Babich
 */
package org.babich.crawler.api;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.StringJoiner;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Base class for page processing messages.
 */
public class ProcessingMessage implements Serializable {

    protected final LocalDateTime time;

    public ProcessingMessage() {
        this.time = LocalDateTime.now();
    }

    public LocalDateTime getTime() {
        return time;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ProcessingMessage.class.getSimpleName() + "[", "]")
                .add("time=" + time)
                .toString();
    }
}
