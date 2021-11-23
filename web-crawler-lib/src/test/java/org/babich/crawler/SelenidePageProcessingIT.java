/*
 * @author Vadim Babich
 */
package org.babich.crawler;

import static com.codeborne.selenide.Selenide.$;
import static org.hamcrest.Matchers.hasSize;

import com.google.common.io.Resources;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import org.babich.crawler.WebCrawler.WebCrawlerBuilder;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.messages.PageProcessingComplete;
import org.babich.crawler.common.ProcessingMessageListener;
import org.babich.crawler.common.TestHelper;
import org.babich.crawler.configuration.exception.CrawlerConfigurationException;
import org.babich.crawler.processing.DefaultSelenidePageProcessing;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By.ById;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class SelenidePageProcessingIT {

    private Path configPath;
    private List<Page> visitedLinks;
    private ProcessingMessageListener listener;

    private static final String containerHost = "nginx";
    public static Network network = Network.newNetwork();

    @Container
    public static NginxContainer<?> nginx = new NginxContainer<>("nginx:alpine")
            .withNetwork(network)
            .withNetworkAliases(containerHost)
            .withClasspathResourceMapping("content"
                    , "/usr/share/nginx/html"
                    , BindMode.READ_ONLY);

    @Container
    public static BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>()
            .withNetwork(network)
            .withCapabilities(new ChromeOptions());


    @BeforeEach
    public void setUp() throws URISyntaxException {
        System.setProperty("selenide.remote", chrome.getSeleniumAddress().toString());
        //noinspection UnstableApiUsage
        configPath = Paths.get(Resources.getResource("sync-crawler.yml").toURI());
        visitedLinks = new LinkedList<>();
        listener = new ProcessingMessageListener();
    }


    @DisplayName("Selenide processing should pass through every domain resource.")
    @Test
    void shouldPassThroughEveryDomainResource_DefaultSelenidePageProcessing() throws CrawlerConfigurationException {

        WebCrawler webCrawler = new WebCrawlerBuilder("dictionary", configPath)
                .startUrl("http://" + containerHost + "/landing.html")
                .eventListeners(listener)
                .useDefaultPageProcessing(new LinkReplacementSelenidePageProcessing(containerHost))
                .pageConsumer(visitedLinks::add)
                .build();

        webCrawler.start();

        Assertions.assertAll(
                () -> Assert.assertThat(visitedLinks, hasSize(5)),
                () -> Assert.assertThat(listener.getProcessed(), hasSize(5)),
                () -> Assert.assertThat(listener.getSkipped(), hasSize(0))
        );
    }

    @DisplayName("The Crawler should be stopped and then started correctly when processing crashed.")
    @Test
    void shouldStoppedAndStartedCorrectlyWhenProcessingCrashed_DefaultSelenidePageProcessing()
            throws CrawlerConfigurationException {

        Predicate<Page> errorPageFilter = page -> page.getPageName().contains("2");

        final WebCrawler crashedCrawler = new WebCrawlerBuilder("dictionary", configPath)
                .startUrl("http://" + containerHost + "/landing.html")
                .eventListeners(listener)
                .withCustomPageProcessing(builder -> builder
                        .forPages(errorPageFilter.negate())
                        .processingBy(new LinkReplacementSelenidePageProcessing(containerHost))
                )
                .withCustomPageProcessing(builder -> builder
                        .forPages(errorPageFilter)
                        .processingBy(new LinkReplacementSelenidePageProcessing(containerHost) {
                            @Override
                            protected void parse(Page page) {
                                super.parse(page);
                                $(new ById("unknown")).getText();
                            }
                        })
                )
                .pageConsumer(visitedLinks::add)
                .build();

        Assertions.assertThrows(Throwable.class, crashedCrawler::start);

        Assertions.assertAll(
                () -> Assert.assertThat(visitedLinks, hasSize(3)),
                () -> Assert.assertThat(listener.getProcessed(), hasSize(3))
        );

        WebCrawler proceedingCrawler = new WebCrawlerBuilder("dictionary", configPath)
                .startUrl("http://" + containerHost + "/landing.html")
                .eventListeners(listener)
                .useDefaultPageProcessing(new LinkReplacementSelenidePageProcessing(containerHost))
                .pageConsumer(visitedLinks::add)
                .build();

        proceedingCrawler.start();

        Assertions.assertAll(
                () -> Assert.assertThat(visitedLinks, hasSize(5)),
                () -> Assert.assertThat(listener.getProcessed(), hasSize(5)),
                () -> Assert.assertThat(listener.getSkipped(), hasSize(0))
        );

        //page sources after a crawler crash should not be deleted
        long countOfExistedPageSources = listener.getProcessed().stream()
                .map(PageProcessingComplete::getPage)
                .map(Page::getPageSource)
                .map(TestHelper::toFile)
                .filter(File::exists)
                .count();
        Assert.assertEquals(3, countOfExistedPageSources);
    }

    public static class LinkReplacementSelenidePageProcessing extends DefaultSelenidePageProcessing {

        final UnaryOperator<String> toContainerUrl;

        public LinkReplacementSelenidePageProcessing(String containerHost) {
            toContainerUrl = TestHelper.replaceUrl(Pattern.compile("//(?<host>localhost)/.+.html"), containerHost);
        }

        @Override
        protected Page toPage(Page page, String dependentLink) {
            dependentLink = toContainerUrl.apply(dependentLink);
            return super.toPage(page, dependentLink);
        }

        @Override
        protected String getHost(String href) {
            href = toContainerUrl.apply(href);
            return super.getHost(href);
        }
    }
}
