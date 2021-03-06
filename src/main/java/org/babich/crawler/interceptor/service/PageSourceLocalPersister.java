/*
 * @author Vadim Babich
 */
package org.babich.crawler.interceptor.service;

import com.google.common.eventbus.Subscribe;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessingInterceptor;
import org.babich.crawler.api.messages.CrawlerStopped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The page source is saved to disk and the {@code Page#pageSource} value is replaced with the file path as a URI string.
 * This is necessary to keep the Page object lightweight and portable.
 */
public class PageSourceLocalPersister implements PageProcessingInterceptor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Path pageSourceDir;


    public PageSourceLocalPersister() {
        try {
            pageSourceDir = Files.createTempDirectory("page_source_persister");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public void afterProcessing(Page page, List<Page> successorPages) {
        String pageName = page.getPageName();
        if (StringUtils.isBlank(page.getPageSource())) {
            logger.info("Page {} source is empty", pageName);
            return;
        }

        try {
            URI pageSourceUri = save(page).toUri();
            page.setPageSource(pageSourceUri.toString());
        } catch (IOException exception) {
            logger.error("The original page {} could not be saved to the file system.", pageName, exception);
        }
    }

    private Path save(Page page) throws IOException {
        Path newFilePath = Files.createTempFile(pageSourceDir, page.getPageName(), StringUtils.EMPTY);
        return Files.write(newFilePath, page.getPageSource().getBytes());
    }

    /**
     * remove page sources from disk when the crawler is stopped.
     */
    @Subscribe
    public void tearDown(CrawlerStopped message){
        File sourceDir = pageSourceDir.toFile();
        if(message.isAbnormal()){
            logger.info("The crawler has crashed, the page sources have not been removed from the directory {}."
                    , sourceDir);
            return;
        }

        if(sourceDir.exists() && FileUtils.deleteQuietly(pageSourceDir.toFile())) {
            logger.debug("Page sources removed from the dir {}.", sourceDir);
            return;
        }

        logger.error("Unable to remove page sources from directory {}.", sourceDir);
    }
}
