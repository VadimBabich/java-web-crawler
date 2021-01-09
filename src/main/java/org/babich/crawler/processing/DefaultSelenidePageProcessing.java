/*
 * @author Vadim Babich
 */
package org.babich.crawler.processing;

import static com.codeborne.selenide.Selenide.$$;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.babich.crawler.api.Page;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSelenidePageProcessing extends AbstractPageProcessing {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());


    public DefaultSelenidePageProcessing() {
    }

    @Override
    protected void parse(Page page) {
        open(page.getPageUrl());
        page.setPageSource(grabPageSource());
    }


    @Override
    protected Collection<String> findSuccessorLinks(Page page) {
        String pageHost = getHost(page.getPageUrl());
        if(StringUtils.isBlank(pageHost)){
            return Collections.emptySet();
        }

        ElementsCollection elementsCollection = $$(By.xpath("//a[@href]"));
        return elementsCollection.stream()
                .map(this::toLink)
                .filter(link -> pageHost.equals(getHost(link)))
                .collect(Collectors.toSet());
    }

    private String toLink(SelenideElement element){
        return element.getAttribute("href");
    }

    protected void open(String url) {
        Selenide.open(url);
    }

    protected String grabPageSource() {
        return WebDriverRunner.getWebDriver().getPageSource();
    }

}
