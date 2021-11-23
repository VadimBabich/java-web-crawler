/*
 * @author Vadim Babich
 */
package org.babich.crawler.interceptor;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessingInterceptor;
import org.babich.crawler.api.ProcessingMessage;
import org.babich.crawler.event.LocalEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomMessagesDispatcher<T extends Function<Page, ProcessingMessage> & Predicate<Page>>
        implements PageProcessingInterceptor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final List<T> customMessageDispatchers;
    private final PageProcessingInterceptor defaultMessageDispatcher;
    private final LocalEventBus eventBus;


    @SafeVarargs
    public CustomMessagesDispatcher(LocalEventBus eventBus
            , PageProcessingInterceptor defaultMessageDispatcher
            , T... pagePreProcessingSeq) {

        if (null == defaultMessageDispatcher) {
            throw new IllegalArgumentException("defaultMessageDispatcher cannot be null");
        }

        this.eventBus = eventBus;

        this.customMessageDispatchers = null == pagePreProcessingSeq ? new LinkedList<>()
                : Arrays.asList(pagePreProcessingSeq.clone());
        this.defaultMessageDispatcher = defaultMessageDispatcher;
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE - 100;
    }


    @Override
    public void afterProcessing(Page page, List<Page> successorPages) {
        Optional<T> dispatcher = customMessageDispatchers.stream()
                .filter(item -> item.test(page))
                .findFirst();

        if (dispatcher.isPresent()) {
            logger.debug("For page {} has been applied the message dispatcher {}.", page.getPageName(), dispatcher);
            eventBus.post(dispatcher.get().apply(new Page(page)));
            return;
        }

        logger.debug("The page {} doesn't match any message dispatcher, "
                        + "has been applied the default message dispatcher."
                , page.getPageName());

        defaultMessageDispatcher.afterProcessing(page, successorPages);
    }
}
