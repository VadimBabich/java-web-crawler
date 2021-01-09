/*
 * @author Vadim Babich
 */
package org.babich.crawler.processing;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessing;

public abstract class AbstractPageProcessing implements PageProcessing {

    @Override
    public Iterable<Page> process(Page page) {
        parse(page);

        Collection<String> dependentLinks = findSuccessorLinks(page);
        if(null == dependentLinks){
            return Collections.emptySet();
        }

        return dependentLinks.stream()
                .map(item -> toPage(page, item))
                .collect(Collectors.toList());
    }

    protected abstract void parse(Page page);

    protected abstract Collection<String> findSuccessorLinks(Page page);

    protected Page toPage(Page page, String dependentLink){
        return new Page(page.getPageContextRef(), page.getCrawlerName(), dependentLink, StringUtils.EMPTY);
    }

    protected String getHost(String href) {
        try {
            return new URL(href).getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
