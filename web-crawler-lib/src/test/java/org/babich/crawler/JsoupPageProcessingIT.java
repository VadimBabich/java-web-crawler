/*
 * @author Vadim Babich
 */
package org.babich.crawler;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;

import com.google.common.io.Resources;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.awaitility.Duration;
import org.babich.crawler.WebCrawler.WebCrawlerBuilder;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessing;
import org.babich.crawler.api.messages.PageProcessingSkippe;
import org.babich.crawler.common.ProcessingMessageListener;
import org.babich.crawler.common.TestHelper;
import org.babich.crawler.common.TestProcessingMessage;
import org.babich.crawler.configuration.exception.CrawlerConfigurationException;
import org.babich.crawler.configuration.processing.CustomPageProcessingConfig;
import org.babich.crawler.processing.DefaultJsoupPageProcessing;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class JsoupPageProcessingIT {

    private Path configPath;
    private String containerHost;
    private List<Page> visitedLinks;
    private ProcessingMessageListener listener;


    @Container
    public static NginxContainer<?> webServer = new NginxContainer<>("nginx:alpine")
            .withClasspathResourceMapping("content"
                    , "/usr/share/nginx/html"
                    , BindMode.READ_ONLY);


    @BeforeEach
    public void setUp() throws URISyntaxException {
        containerHost = webServer.getHost() + ":" + webServer.getFirstMappedPort();
        configPath = Paths.get(Resources.getResource("sync-crawler.yml").toURI());
        visitedLinks = new LinkedList<>();
        listener = new ProcessingMessageListener();
    }


    @DisplayName("Jsoup processing should pass through every domain resource.")
    @Test
    void shouldPassThroughEveryDomainResource_DefaultJsoupPageProcessing() throws CrawlerConfigurationException {

        WebCrawler webCrawler = new WebCrawlerBuilder("dictionary", configPath)
                .startUrl("http://" + containerHost + "/landing.html")
                .eventListeners(listener)
                .withCustomPageProcessing(builder -> builder
                        .forPages(CustomPageProcessingConfig.Matchers.alwaysTrue())
                        .processingBy(new LinkReplacementJsoupPageProcessing(containerHost))
                        .skippingPages(when -> when
                                .conditionIsTrue(page -> false)
                                .andSendMessage(PageProcessingSkippe::new))
                )
                .pageConsumer(visitedLinks::add)
                .build();

        webCrawler.start();

        Assertions.assertAll(
                () -> MatcherAssert.assertThat(visitedLinks, hasSize(5)),
                () -> MatcherAssert.assertThat(listener.getProcessed(), hasSize(5)),
                () -> MatcherAssert.assertThat(listener.getSkipped(), hasSize(0))
        );
    }

    @DisplayName("The Crawler should pass through all domain resources less than the first depth.")
    @Test
    void shouldPassThroughAllDomainResourcesLessThanFirstDepth_DefaultJsoupPageProcessing()
            throws CrawlerConfigurationException {

        WebCrawler webCrawler = new WebCrawlerBuilder("dictionary", configPath)
                .startUrl("http://" + containerHost + "/landing.html")
                .maxDepth(1)
                .eventListeners(listener)
                .withCustomPageProcessing(builder -> builder
                        .forPages(CustomPageProcessingConfig.Matchers.alwaysTrue())
                        .processingBy(new LinkReplacementJsoupPageProcessing(containerHost))
                        .skippingPages(when -> when
                                .conditionIsTrue(page -> false)
                                .andSendMessage(PageProcessingSkippe::new))
                )
                .pageConsumer(visitedLinks::add)
                .build();

        webCrawler.start();

        Assertions.assertAll(
                () -> MatcherAssert.assertThat(visitedLinks, hasSize(5)),
                () -> MatcherAssert.assertThat(listener.getProcessed(), hasSize(4)),
                () -> MatcherAssert.assertThat(listener.getSkipped(), hasSize(1))
        );
    }

    @DisplayName("The Crawler should receive a custom message.")
    @Test
    void shouldBeReceivedCustomMessage_DefaultJsoupPageProcessing() throws CrawlerConfigurationException {

        Predicate<Page> errorPageFilter = page -> page.getPageName().contains("2");
        PageProcessing pageProcessing = new LinkReplacementJsoupPageProcessing(containerHost);

        WebCrawler webCrawler = new WebCrawlerBuilder("dictionary", configPath)
                .startUrl("http://" + containerHost + "/landing.html")
                .eventListeners(listener)
                .useDefaultPageProcessing(pageProcessing)
                .withCustomPageProcessing(builder -> builder
                        .forPages(errorPageFilter)
                        .sendMessage(TestProcessingMessage::new)
                        .processingBy(pageProcessing)
                )
                .pageConsumer(visitedLinks::add)
                .build();

        webCrawler.start();

        Assertions.assertAll(
                () -> MatcherAssert.assertThat(visitedLinks, hasSize(5)),
                () -> MatcherAssert.assertThat(listener.getProcessed(), hasSize(4)),
                () -> MatcherAssert.assertThat(listener.getTestMessages(), hasSize(1))
        );
    }

    @DisplayName("The Crawler should receive asynchronous messages.")
    @Test
    void shouldReceiveAsynchronousMessages_DefaultJsoupPageProcessing() throws CrawlerConfigurationException {

        WebCrawler webCrawler = new WebCrawlerBuilder("dictionary")
                .startUrl("http://" + containerHost + "/landing.html")
                .eventListeners(listener)
                .useDefaultPageProcessing(new LinkReplacementJsoupPageProcessing(containerHost))
                .pageConsumer(visitedLinks::add)
                .build();

        webCrawler.start();

        await().atMost(Duration.TEN_SECONDS)
                .until(listener::getCrawlerStops, hasSize(1));

        Assertions.assertAll(
                () -> MatcherAssert.assertThat(listener.getCrawlerStarts(), hasSize(1)),
                () -> MatcherAssert.assertThat(visitedLinks, hasSize(5)),
                () -> MatcherAssert.assertThat(listener.getProcessed(), hasSize(5)),
                () -> MatcherAssert.assertThat(listener.getSuccessorsProcessingCompletes(), hasSize(2)),
                () -> MatcherAssert.assertThat(listener.getTestMessages(), hasSize(0)),
                () -> MatcherAssert.assertThat(listener.getSkipped(), hasSize(0))
        );
    }

    @DisplayName("The Crawler should extract structured data from pages")
    @Test
    void shouldExtractStructuredDataFromPages_DefaultJsoupPageProcessing() throws CrawlerConfigurationException {

        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("München", StringUtils.EMPTY);
        expectedMap.put("Merkzettel", "a book of paper for writing on.");
        expectedMap
                .put("Übersicht", "a short description of something that provides general information but no details.");
        expectedMap.put("Nachrichten", "a book of paper for writing on.");
        expectedMap.put("Technologien", "the methods for using scientific discoveries for practical purposes.");

        WebCrawler webCrawler = new WebCrawlerBuilder("dictionary", configPath)
                .startUrl("http://" + containerHost + "/landing.html")
                .eventListeners(listener)
                .withCustomPageProcessing(builder -> builder
                        .forPages(CustomPageProcessingConfig.Matchers.alwaysTrue())
                        .sendMessage(TestProcessingMessage::new)
                        .processingBy(new LinkReplacementJsoupPageProcessing(containerHost) {
                            @Override
                            protected void parse(Page page) {
                                super.parse(page);
                                page.setPayload(Pair.of(doc.title(), doc.selectFirst("div#payload").text()));
                            }
                        })
                )
                .pageConsumer(visitedLinks::add)
                .build();

        webCrawler.start();

        MatcherAssert.assertThat(visitedLinks, hasSize(5));
        Map<String, String> extractedData = visitedLinks.stream()
                .map(Page::getPayload)
                .map(Pair.class::cast)
                .collect(Collectors.toMap(pair -> (String) pair.getKey(), pair -> (String) pair.getValue()));

        extractedData.forEach((k, v) -> MatcherAssert.assertThat(expectedMap, IsMapContaining.hasEntry(k, v)));

        MatcherAssert.assertThat(listener.getTestMessages(), hasSize(5));
        listener.getTestMessages().stream()
                .map(TestProcessingMessage::getPayload)
                .forEach(pair -> MatcherAssert.assertThat(expectedMap
                        , IsMapContaining.hasEntry(pair.getKey(), pair.getValue())));
    }

    static class LinkReplacementJsoupPageProcessing extends DefaultJsoupPageProcessing {

        final UnaryOperator<String> toContainerUrl;

        public LinkReplacementJsoupPageProcessing(String containerHost) {
            toContainerUrl = TestHelper.replaceUrl(Pattern.compile("//(?<host>localhost)/.+.html"), containerHost);
        }

        @Override
        protected Page toPage(Page page, String dependentLink) {
            dependentLink = toContainerUrl.apply(dependentLink);
            return super.toPage(page, dependentLink);
        }
    }

}
