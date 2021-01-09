/*
 * @author Vadim Babich
 */
package org.babich.crawler.interceptor.service;

import com.google.common.eventbus.Subscribe;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessingInterceptor;
import org.babich.crawler.api.messages.PageRecovered;
import org.babich.crawler.configuration.ApplicationConfig.PageConfig;

public class SuccessorPagesPostProcessing implements PageProcessingInterceptor {

    private final AtomicInteger pageNumber = new AtomicInteger(0);
    private PageConfig pageConfig;

    public SuccessorPagesPostProcessing() {
    }

    public SuccessorPagesPostProcessing(PageConfig pageConfig) {
        this.pageConfig = pageConfig;
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE - 1;
    }

    @Override
    public void afterProcessing(Page page, List<Page> successorPages) {
        int count = null == successorPages ? 0 : successorPages.size();

        setNameForSuccessorPagesIfAbsent(successorPages, count);
        depthIncrementForSuccessorPages(page, successorPages);
        updateStatistic(page, count);
    }

    void depthIncrementForSuccessorPages(Page page, List<Page> successorPages) {
        if (null == successorPages) {
            return;
        }

        int depth = page.getDepth() + 1;
        successorPages.forEach(p -> p.setDepth(depth));
    }

    void setNameForSuccessorPagesIfAbsent(List<Page> returnedValue, int count) {
        if (0 == count) {
            return;
        }
        for (int number = pageNumber.getAndUpdate(value -> value + count), i = 0; i < count; i++, number++) {
            Page p = returnedValue.get(i);
            if (StringUtils.isNotBlank(p.getPageName())) {
                continue;
            }

            p.setPageName(StrSubstitutor.replace(pageConfig.getPageNamePattern()
                    , Collections.singletonMap("number", number)));
        }
    }

    void updateStatistic(Page page, int count) {
        page.getPageContextRef().updateAndGet(pageContext -> pageContext.transform(builder -> {
            builder.pagesProcessed(pageContext.getPagesProcessed() + 1);
            builder.pageCount(pageContext.getPageCount() + count);
        }));
    }

    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void pageOnRecovered(PageRecovered message) {
        pageNumber.incrementAndGet();
    }
}
