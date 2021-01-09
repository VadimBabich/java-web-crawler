/*
 * @author Vadim Babich
 */
package org.babich.crawler.interceptor;

import java.util.List;

import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessingInterceptor;
import org.babich.crawler.api.messages.PageProcessingComplete;
import org.babich.crawler.api.messages.PageProcessingStart;
import org.babich.crawler.event.LocalEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class DefaultMessageProducer implements PageProcessingInterceptor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private LocalEventBus eventBus;

    public DefaultMessageProducer() {
    }

    public DefaultMessageProducer(LocalEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE + 1;
    }

    @Override
    public void beforeProcessing(Page page) {
        logger.debug("Processing for page {} started.", page.getPageName());
        if(null == eventBus){
            return;
        }

        eventBus.post(new PageProcessingStart(new Page(page)));
    }

    @Override
    public void afterProcessing(Page page, List<Page> successorPages) {
        logger.debug("Processing for page {} completed.", page.getPageName());
        if(null == eventBus){
            return;
        }

        eventBus.post(new PageProcessingComplete(new Page(page)));
    }

}
