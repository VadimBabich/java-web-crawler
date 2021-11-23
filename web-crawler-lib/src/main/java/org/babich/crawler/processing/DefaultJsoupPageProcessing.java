/*
 * @author Vadim Babich
 */
package org.babich.crawler.processing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.babich.crawler.api.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultJsoupPageProcessing extends AbstractPageProcessing {

    private final Logger logger = LoggerFactory.getLogger(DefaultJsoupPageProcessing.class);
    protected Document doc;

    @Override
    protected void parse(Page page) {
        try {
            doc = Jsoup.connect(page.getPageUrl()).get();
            page.setPageSource(doc.html());
        } catch (IOException exception) {
            logger.error("Unable to parse page by url {} ", page.getPageUrl());
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    protected Collection<String> findSuccessorLinks(Page page) {
        String pageHost = getHost(page.getPageUrl());
        if(StringUtils.isBlank(pageHost)){
            return Collections.emptySet();
        }

        return doc.select("a[href]").stream()
                .map(this::toLink)
                .filter(link -> pageHost.equals(getHost(link)))
                .collect(Collectors.toSet());
    }

    private String toLink(Element element){
        return element.attr("abs:href");
    }

}
