package org.babich.crawler.interceptor.service;

import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageContext;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;

class PageUrlNormalizerTest {

    PageUrlNormalizer underTest;

    @BeforeEach
    public void setup(){
        underTest = new PageUrlNormalizer();
    }

    @Test
    void givenUrlWithDotSegmentWhenNormalizationThenRemovingDotSegmentsInResult(){
        PageContext context = Mockito.mock(PageContext.class);

        Page page = new Page(new AtomicReference<>(context)
                ,"test_crawler", "http://example.com/foo/./bar/baz/../qux", "page_name");
        underTest.beforeProcessing(page);

        Assert.assertEquals("http://example.com/foo/bar/qux", page.getPageUrl());
    }

    @Test
    void givenUrlWithDuplicateSlashesWhenNormalizationThenRemovingDuplicateSlashesInResult(){
        PageContext context = Mockito.mock(PageContext.class);

        Page page = new Page(new AtomicReference<>(context)
                ,"test_crawler", "http://example.com/foo//bar.html", "page_name");
        underTest.beforeProcessing(page);

        Assert.assertEquals("http://example.com/foo/bar.html", page.getPageUrl());
    }

    @Test
    void givenUrlSchemeAndHostInUpperCaseWhenNormalizationThenSchemeAndHostToLowercaseInResult(){
        PageContext context = Mockito.mock(PageContext.class);

        Page page = new Page(new AtomicReference<>(context)
                ,"test_crawler", "HTTP://User@Example.COM/Foo", "page_name");
        underTest.beforeProcessing(page);

        Assert.assertEquals("http://User@example.com/Foo", page.getPageUrl());
    }

    @Test
    void givenUrlUnsortedQueryParametersWhenNormalizationThenSortedQueryParametersInResult(){
        PageContext context = Mockito.mock(PageContext.class);

        Page page = new Page(new AtomicReference<>(context)
                ,"test_crawler", "http://example.com/display?lang=en&article=fred", "page_name");
        underTest.beforeProcessing(page);

        Assert.assertEquals("http://example.com/display?article=fred&lang=en", page.getPageUrl());
    }

    @Test
    void givenUrlWithFragmentWhenNormalizationThenWithoutFragmentInResult(){
        PageContext context = Mockito.mock(PageContext.class);

        Page page = new Page(new AtomicReference<>(context)
                ,"test_crawler", "http://example.com/bar.html#section1", "page_name");
        underTest.beforeProcessing(page);

        Assert.assertEquals("http://example.com/bar.html", page.getPageUrl());
    }

    @Test
    void givenUrlWithDefaultPortWhenNormalizationThenWithoutPortInResult(){
        PageContext context = Mockito.mock(PageContext.class);

        Page page = new Page(new AtomicReference<>(context)
                ,"test_crawler", "http://example.com:80/bar.html#section1", "page_name");
        underTest.beforeProcessing(page);

        Assert.assertEquals("http://example.com/bar.html", page.getPageUrl());
    }

    @Test
    void givenUrlWithEmptyQueryParamsWhenNormalizationThenQueryMarkRemovedInResult(){
        PageContext context = Mockito.mock(PageContext.class);

        Page page = new Page(new AtomicReference<>(context)
                ,"test_crawler", "http://example.com/display?", "page_name");
        underTest.beforeProcessing(page);

        Assert.assertEquals("http://example.com/display", page.getPageUrl());
    }


}