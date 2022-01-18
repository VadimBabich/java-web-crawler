/*
 * @author Vadim Babich
 */
package org.babich.crawler.interceptor.service;

import org.apache.commons.lang3.StringUtils;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessingInterceptor;

import java.util.List;

/**
 * This class sets the size of page's source code in bytes. This interceptor should be fired with the highest priority
 * before any changes to the page.
 */
public class PageSizeInitializer implements PageProcessingInterceptor {

    final int order = Integer.MIN_VALUE + 100;

    PageSizeInitializer() {
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public void afterProcessing(Page page, List<Page> successorPages) {
        String pageSource = page.getPageSource();
        if (StringUtils.isBlank(pageSource)) {
            page.setSize(0);
        }

        page.setSize(pageSource.getBytes().length);
    }
}
