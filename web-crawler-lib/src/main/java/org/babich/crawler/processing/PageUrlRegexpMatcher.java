/*
 * @author Vadim Babich
 */
package org.babich.crawler.processing;

import java.util.regex.Pattern;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessing;
import org.babich.crawler.api.processing.AssignedPageProcessing;


public class PageUrlRegexpMatcher implements AssignedPageProcessing {

    private Pattern pattern;

    private PageProcessing delegate;

    private PageUrlRegexpMatcher() {
    }

    public PageUrlRegexpMatcher(String expression, PageProcessing delegate) {
        this.pattern = Pattern.compile(expression);
        this.delegate = delegate;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public PageProcessing getDelegate() {
        return delegate;
    }

    @Override
    public boolean matches(Page page) {
        return pattern.matcher(page.getPageUrl()).matches();
    }

    @Override
    public Iterable<Page> process(Page page) {
        return delegate.process(page);
    }
}
