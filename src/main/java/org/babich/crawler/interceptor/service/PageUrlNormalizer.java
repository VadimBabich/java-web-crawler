package org.babich.crawler.interceptor.service;

import com.google.common.base.Strings;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;


public class PageUrlNormalizer implements PageProcessingInterceptor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void beforeProcessing(Page page) {
        String pageUrl = page.getPageUrl();
        if(Strings.isNullOrEmpty(pageUrl)){
            return;
        }

        try {
            URI uri = new URI(pageUrl).normalize();
            page.setPageUrl(uri.toString());
        } catch (URISyntaxException e) {
            logger.error("Cannot parse URL {} string for te page {}."
                    , pageUrl, page.getPageName(), e);
        }
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE + 100;
    }
}
