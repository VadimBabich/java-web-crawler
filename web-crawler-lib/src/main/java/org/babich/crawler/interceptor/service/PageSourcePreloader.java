/*
 * @author Vadim Babich
 */
package org.babich.crawler.interceptor.service;

import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessingInterceptor;
import org.babich.crawler.configuration.ApplicationConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Loading page content from successor URLs.
 * This feature can greatly improve crawling speed by using a parallel
 * data loading approach. This mean all successor URLs have to be validated because they will all be loaded.
 */
public class PageSourcePreloader implements PageProcessingInterceptor {

    final protected Logger logger = LoggerFactory.getLogger(this.getClass());

    final int order = Integer.MAX_VALUE - 100;

    private final ExecutorService service;
    private final ApplicationConfig.PageConfig config;

    private PageSourcePreloader() {
        this.service = null;
        this.config = null;
    }

    public PageSourcePreloader(ApplicationConfig.PageConfig config, Integer capacity) {
        this.service = Executors.newFixedThreadPool(capacity);
        this.config = config;
        setupShutdownHook();
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public void afterProcessing(Page page, List<Page> successorPages) {
        if(isPagePreloadFeatureDisabled()){
            return;
        }

        successorPages.forEach(p -> service.submit(() -> {
            Document doc;
            try {
                doc = Jsoup.connect(p.getPageUrl()).userAgent(config.getUserAgent()).get();
                p.setPageSource(doc.html());
            } catch (IOException e) {
                logger.error("Error occurred while preloading the page from the URL:" + p.getPageUrl(), e);
                throw new UncheckedIOException(e);
            }
        }));
    }

    boolean isPagePreloadFeatureDisabled(){
        return !config.getPreLoad();
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if(!service.awaitTermination(3, TimeUnit.SECONDS)){
                    service.shutdown();
                }
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }));
    }
}
