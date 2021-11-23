/*
 * @author Vadim Babich
 */
package org.babich.crawler.event;

import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventBusExceptionHandler implements SubscriberExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void handleException(Throwable exception, SubscriberExceptionContext context) {

        logger.debug("The message {} got an error in the listener {}."
                , context.getEvent().getClass()
                , context.getSubscriberMethod()
                , exception);
    }
}
