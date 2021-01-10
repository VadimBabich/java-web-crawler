/*
 * @author Vadim Babich
 */
package org.babich.crawler.common;

import org.apache.commons.lang3.tuple.Pair;
import org.babich.crawler.api.Page;
import org.babich.crawler.configuration.processing.CustomProcessingMessage;


public class TestProcessingMessage extends CustomProcessingMessage {

    private Pair<String, String> payload;

    public TestProcessingMessage(Page page) {
        super(page);
        if (null == page.getPayload()) {
            return;
        }
        //noinspection unchecked
        payload = (Pair<String, String>) page.getPayload();
    }

    public Pair<String, String> getPayload() {
        return payload;
    }
}
