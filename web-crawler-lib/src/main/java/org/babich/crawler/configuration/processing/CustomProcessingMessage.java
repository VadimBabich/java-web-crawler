/*
 * @author Vadim Babich
 */
package org.babich.crawler.configuration.processing;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.ProcessingMessage;

/**
 * Base class used for all types of custom messages
 */
public class CustomProcessingMessage extends ProcessingMessage {

    private final Page page;

    public CustomProcessingMessage(Page page) {
        this.page = page;
    }

    public Page getPage() {
        return page;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
