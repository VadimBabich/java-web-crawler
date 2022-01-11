/*
 * @author Vadim Babich
 */
package org.babich.crawler.processing;

import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessing;
import org.babich.crawler.api.processing.AssignedPageProcessing;
import org.babich.crawler.configuration.exception.PreProcessingChainException;
import org.babich.crawler.metrics.PageProcessingMetricsWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CombinePageProcessing<T extends AssignedPageProcessing> implements PageProcessing {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final List<PageProcessingMetricsWrapper> pageProcessingFlow;
    private final PageProcessing defaultPageProcessing;

    @SafeVarargs
    public CombinePageProcessing(PageProcessing defaultPageProcessing, T... pageProcessingFlow) {
        if(null == defaultPageProcessing){
            throw new IllegalArgumentException("defaultPageProcessingFlow cannot be null");
        }

        this.pageProcessingFlow = null == pageProcessingFlow ? new LinkedList<>()
                : wrap(pageProcessingFlow);
        this.defaultPageProcessing = PageProcessingMetricsWrapper.of(defaultPageProcessing);
    }

    @SafeVarargs
    final List<PageProcessingMetricsWrapper> wrap(T... pageProcessingFlow){
        return Arrays.stream(pageProcessingFlow).map(PageProcessingMetricsWrapper::new).collect(Collectors.toList());
    }

    @Override
    public Iterable<Page> process(Page page) {
        Optional<PageProcessingMetricsWrapper> flow = pageProcessingFlow.stream()
                .filter(item -> item.test(page))
                .findFirst();

        if(flow.isPresent()){
            logger.debug("For page {} has been applied the processing flow {}.", page.getPageName(), flow);
            return flow.get().process(page);
        }

        logger.debug("The page {} doesn't match any pattern, has been applied the default page processing flow."
                , page.getPageName());

        if(null == defaultPageProcessing){
            throw new PreProcessingChainException("No processing assigned to the page");
        }

        return defaultPageProcessing.process(page);
    }

}
