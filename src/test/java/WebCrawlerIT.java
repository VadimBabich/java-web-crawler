/*
 * @author Vadim Babich
 */
import static com.codeborne.selenide.Selenide.$;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;

import com.google.common.eventbus.Subscribe;
import com.google.common.io.Resources;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.awaitility.Duration;
import org.babich.crawler.WebCrawler;
import org.babich.crawler.WebCrawler.WebCrawlerBuilder;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessing;
import org.babich.crawler.api.messages.CrawlerStarted;
import org.babich.crawler.api.messages.CrawlerStopped;
import org.babich.crawler.api.messages.PageProcessingComplete;
import org.babich.crawler.api.messages.PageProcessingSkippe;
import org.babich.crawler.api.messages.SuccessorsProcessingComplete;
import org.babich.crawler.configuration.exception.CrawlerConfigurationException;
import org.babich.crawler.configuration.processing.CustomPageProcessingConfig;
import org.babich.crawler.configuration.processing.CustomProcessingMessage;
import org.babich.crawler.interceptor.service.JdkSerializeBackupService;
import org.babich.crawler.processing.DefaultJsoupPageProcessing;
import org.babich.crawler.processing.DefaultSelenidePageProcessing;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By.ById;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class WebCrawlerIT {

    private Path configPath;
    private String containerHost;
    private List<Page> visitedLinks;
    private ProcessingMessageListener listener;
    private final Preferences preferences = Preferences.userNodeForPackage(JdkSerializeBackupService.class);

    @Container
    public static NginxContainer<?> webServer = new NginxContainer<>("nginx:alpine")
            .withClasspathResourceMapping("content"
                    , "/usr/share/nginx/html"
                    , BindMode.READ_ONLY);


    @BeforeEach
    public void setUp() throws URISyntaxException {
        containerHost = webServer.getHost() + ":" + webServer.getFirstMappedPort();
        configPath = Paths.get(Resources.getResource(getClass(), "sync-crawler.yml").toURI());
        visitedLinks = new LinkedList<>();
        listener = new ProcessingMessageListener();
    }

    @AfterEach
    public void tearDown() {
        preferences.remove("backupPath");
    }


    @DisplayName("Jsoup processing should pass through every domain resource.")
    @Test
    void shouldPassThroughEveryDomainResource_DefaultJsoupPageProcessing() throws CrawlerConfigurationException {

        WebCrawler webCrawler = new WebCrawlerBuilder("dictionary")
                .startUrl("http://" + containerHost + "/landing.html")
                .configurationPath(configPath)
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
                () -> Assert.assertThat(visitedLinks, hasSize(5)),
                () -> Assert.assertThat(listener.getProcessed(), hasSize(5)),
                () -> Assert.assertThat(listener.getSkipped(), hasSize(0))
        );
    }


    @DisplayName("Selenide processing should pass through every domain resource.")
    @Test
    void shouldPassThroughEveryDomainResource_DefaultSelenidePageProcessing() throws CrawlerConfigurationException {

        WebCrawler webCrawler = new WebCrawlerBuilder("dictionary")
                .startUrl("http://" + containerHost + "/landing.html")
                .configurationPath(configPath)
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

        final WebCrawler crashedCrawler = new WebCrawlerBuilder("dictionary")
                .startUrl("http://" + containerHost + "/landing.html")
                .eventListeners(listener)
                .configurationPath(configPath)
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

        WebCrawler proceedingCrawler = new WebCrawlerBuilder("dictionary")
                .startUrl("http://" + containerHost + "/landing.html")
                .eventListeners(listener)
                .configurationPath(configPath)
                .useDefaultPageProcessing(new LinkReplacementSelenidePageProcessing(containerHost))
                .pageConsumer(visitedLinks::add)
                .build();

        proceedingCrawler.start();

        Assertions.assertAll(
                () -> Assert.assertThat(visitedLinks, hasSize(5)),
                () -> Assert.assertThat(listener.getProcessed(), hasSize(5)),
                () -> Assert.assertThat(listener.getSkipped(), hasSize(0))
        );
    }

    @DisplayName("The Crawler should pass through all domain resources less than the first depth.")
    @Test
    void shouldPassThroughAllDomainResourcesLessThanFirstDepth_DefaultJsoupPageProcessing()
            throws CrawlerConfigurationException {

        WebCrawler webCrawler = new WebCrawlerBuilder("dictionary")
                .startUrl("http://" + containerHost + "/landing.html")
                .maxDepth(1)
                .eventListeners(listener)
                .configurationPath(configPath)
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
                () -> Assert.assertThat(visitedLinks, hasSize(5)),
                () -> Assert.assertThat(listener.getProcessed(), hasSize(4)),
                () -> Assert.assertThat(listener.getSkipped(), hasSize(1))
        );
    }

    @DisplayName("The Crawler should receive a custom message.")
    @Test
    void shouldBeReceivedCustomMessage_DefaultJsoupPageProcessing() throws CrawlerConfigurationException {

        Predicate<Page> errorPageFilter = page -> page.getPageName().contains("2");
        PageProcessing pageProcessing = new LinkReplacementJsoupPageProcessing(containerHost);

        WebCrawler webCrawler = new WebCrawlerBuilder("dictionary")
                .startUrl("http://" + containerHost + "/landing.html")
                .eventListeners(listener)
                .configurationPath(configPath)
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
                () -> Assert.assertThat(visitedLinks, hasSize(5)),
                () -> Assert.assertThat(listener.getProcessed(), hasSize(4)),
                () -> Assert.assertThat(listener.getTestMessages(), hasSize(1))
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
                () -> Assert.assertThat(listener.getCrawlerStarts(), hasSize(1)),
                () -> Assert.assertThat(visitedLinks, hasSize(5)),
                () -> Assert.assertThat(listener.getProcessed(), hasSize(5)),
                () -> Assert.assertThat(listener.getSuccessorsProcessingCompletes(), hasSize(2)),
                () -> Assert.assertThat(listener.getTestMessages(), hasSize(0)),
                () -> Assert.assertThat(listener.getSkipped(), hasSize(0))
        );
    }

    @DisplayName("The Crawler should extract structured data from pages")
    @Test
    void shouldExtractStructuredDataFromPages_DefaultJsoupPageProcessing() throws CrawlerConfigurationException {

        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("München", StringUtils.EMPTY);
        expectedMap.put("Merkzettel", "a book of paper for writing on.");
        expectedMap.put("Übersicht", "a short description of something that provides general information but no details.");
        expectedMap.put("Nachrichten", "a book of paper for writing on.");
        expectedMap.put("Technologien", "the methods for using scientific discoveries for practical purposes.");


        WebCrawler webCrawler = new WebCrawlerBuilder("dictionary")
                .startUrl("http://" + containerHost + "/landing.html")
                .configurationPath(configPath)
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

        Assert.assertThat(visitedLinks, hasSize(5));
        Map<String, String> extractedData = visitedLinks.stream()
                .map(Page::getPayload)
                .map(Pair.class::cast)
                .collect(Collectors.toMap(pair -> (String)pair.getKey(), pair -> (String)pair.getValue()));

        extractedData.forEach((k,v) -> Assert.assertThat(expectedMap, IsMapContaining.hasEntry(k, v)));

        Assert.assertThat(listener.getTestMessages(), hasSize(5));
        listener.getTestMessages().stream()
                .map(TestProcessingMessage::getPayload)
                .forEach(pair -> Assert.assertThat(expectedMap
                        , IsMapContaining.hasEntry(pair.getKey(), pair.getValue())));
    }


    static private UnaryOperator<String> replaceUrl(Pattern pattern, String replacement) {
        return url -> {
            Matcher matcher = pattern.matcher(url);
            if (!matcher.find()) {
                return url;
            }
            return new StringBuilder(url)
                    .replace(matcher.start("host"), matcher.end("host"), replacement)
                    .toString();
        };
    }

    static class LinkReplacementJsoupPageProcessing extends DefaultJsoupPageProcessing {

        final UnaryOperator<String> toContainerUrl;

        public LinkReplacementJsoupPageProcessing(String containerHost) {
            toContainerUrl = replaceUrl(Pattern.compile("//(?<host>localhost)/.+.html"), containerHost);
        }

        @Override
        protected Page toPage(Page page, String dependentLink) {
            dependentLink = toContainerUrl.apply(dependentLink);
            return super.toPage(page, dependentLink);
        }
    }

    static class LinkReplacementSelenidePageProcessing extends DefaultSelenidePageProcessing {

        final UnaryOperator<String> toContainerUrl;

        public LinkReplacementSelenidePageProcessing(String containerHost) {
            toContainerUrl = replaceUrl(Pattern.compile("//(?<host>localhost)/.+.html"), containerHost);
        }

        @Override
        protected Page toPage(Page page, String dependentLink) {
            dependentLink = toContainerUrl.apply(dependentLink);
            return super.toPage(page, dependentLink);
        }
    }

    static public class TestProcessingMessage extends CustomProcessingMessage {

        private Pair<String, String> payload;

        public TestProcessingMessage(Page page) {
            super(page);
            if(null == page.getPayload()){
                return;
            }
            payload = (Pair<String, String>) page.getPayload();
        }

        public Pair<String, String> getPayload() {
            return payload;
        }
    }

    public static class ProcessingMessageListener {

        private final List<PageProcessingComplete> processedPagesMessages = new LinkedList<>();
        private final List<PageProcessingSkippe> skippedPagesMessages = new LinkedList<>();
        private final List<TestProcessingMessage> testProcessingMessage = new LinkedList<>();
        private final List<CrawlerStarted> crawlerStarts = new LinkedList<>();
        private final List<CrawlerStopped> crawlerStops = new LinkedList<>();
        private final List<SuccessorsProcessingComplete> successorsProcessingCompletes = new LinkedList<>();


        @Subscribe
        public void OnTest(TestProcessingMessage message) {
            testProcessingMessage.add(message);
        }

        @Subscribe
        public void OnProcess(PageProcessingComplete message) {
            processedPagesMessages.add(message);
        }

        @Subscribe
        public void onSkip(PageProcessingSkippe message) {
            skippedPagesMessages.add(message);
        }

        @Subscribe
        public void onStart(CrawlerStarted message) {
            crawlerStarts.add(message);
        }

        @Subscribe
        public void onStop(CrawlerStopped message) {
            crawlerStops.add(message);
        }

        @Subscribe
        public void onSuccessorsComplete(SuccessorsProcessingComplete message) {
            successorsProcessingCompletes.add(message);
        }


        public List<TestProcessingMessage> getTestMessages() {
            return Collections.unmodifiableList(testProcessingMessage);
        }

        public List<PageProcessingComplete> getProcessed() {
            return Collections.unmodifiableList(processedPagesMessages);
        }

        public List<PageProcessingSkippe> getSkipped() {
            return Collections.unmodifiableList(skippedPagesMessages);
        }

        public List<CrawlerStarted> getCrawlerStarts() {
            return crawlerStarts;
        }

        public List<CrawlerStopped> getCrawlerStops() {
            return crawlerStops;
        }

        public List<SuccessorsProcessingComplete> getSuccessorsProcessingCompletes() {
            return successorsProcessingCompletes;
        }
    }

}
