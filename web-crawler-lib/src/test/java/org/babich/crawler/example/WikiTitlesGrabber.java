package org.babich.crawler.example;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.lang3.StringUtils;
import org.babich.crawler.WebCrawler;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.messages.PageProcessingComplete;
import org.babich.crawler.configuration.ApplicationConfig;
import org.babich.crawler.configuration.exception.CrawlerConfigurationException;
import org.babich.crawler.configuration.processing.CustomPageProcessingConfig;
import org.babich.crawler.processing.DefaultJsoupPageProcessing;
import org.jsoup.nodes.Element;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Finding all wiki pages by google search and extract their titles into PrintStream.
 */
public class WikiTitlesGrabber {

    private PrintStream printStream = System.out;

    public static void main(String[] args) {
        WikiTitlesGrabber crawler = new WikiTitlesGrabber();
        try {
            String searchStr = args.length < 1 || StringUtils.isBlank(args[0])
                    ? "web+crawler" : args[0];
            crawler.findWikiPageAndGrabTitle(searchStr);
        } catch (CrawlerConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * The custom message sent by the crawler when analyzing each wiki page.
     */
    public static class WikiProcessingMessage extends PageProcessingComplete {

        //extracted wiki page titles
        private final List<String> tocTitleList;

        public WikiProcessingMessage(Page page) {
            super(page);
            this.tocTitleList = null == page.getPayload() ? Collections.emptyList()
                    : (List<String>) page.getPayload();
        }

        public List<String> getTocTitleList() {
            return Collections.unmodifiableList(tocTitleList);
        }
    }

    /**
     * subscriber method for receiving wiki page processing messages.
     * @param message contains data extracted from the wiki page.
     */
    @Subscribe
    public void OnWikiPageProcessed(WikiProcessingMessage message){
        List<String> tocTitleList = message.getTocTitleList();
        if(tocTitleList.isEmpty()){
            return;
        }
        printStream.println(message.getPage().getPageUrl());
        tocTitleList.forEach(title -> printStream.println("\t" + title));
    }

    /**
     * Collecting titles on all wiki pages
     * @param searchStr the string to be used by Google as the search string.
     */
    public void findWikiPageAndGrabTitle(String searchStr) throws CrawlerConfigurationException {

        //configuring crawler
        WebCrawler crawler = new WebCrawler.WebCrawlerBuilder("google")
                .maxDepth(1)                            //how far to go from google search results page
                .eventListeners(this)                   //an object containing subscriber methods
                .startUrl("https://www.google.com/search?q=" + searchStr)

                .withCustomPageProcessing(builder -> builder    //configuring search result processing
                        .forPages(createGoogleSearchResultsPagePredicate(builder.getApplicationConfig()))   //only for search result page
                        .processingBy(new DefaultJsoupPageProcessing() {
                            /**
                             * find in the results all links to the wikipedia.org resource
                             */
                            @Override
                            protected Collection<String> findSuccessorLinks(Page page) {
                                return doc.selectFirst("div#search").select("a[href*=wikipedia.org]")
                                        .stream()
                                        .map(element -> element.attr("abs:href"))
                                        .collect(Collectors.toSet());
                            }
                        })
                )
                .withCustomPageProcessing(builder -> builder        //configuring wiki page processing
                        .forPages(CustomPageProcessingConfig.Matchers
                                .hostNameMatcher(".*wikipedia.org"))//only for wikipedia pages
                        .sendMessage(WikiProcessingMessage::new)
                        .processingBy(new DefaultJsoupPageProcessing() {
                            /**
                             *extracting the wiki page titles and putting them into the page payload
                             *  as a serializable list
                             */
                            @Override
                            protected void parse(Page page) {
                                super.parse(page);
                                ArrayList<String> tocTitleList = new ArrayList<>();
                                doc.select("div#toc li").stream()
                                        .map(Element::text)
                                        .forEach(tocTitleList::add);
                                page.setPayload(tocTitleList);
                            }
                        })
                )
                .build();

        crawler.start();
    }

  /**
   * Create predicate for google search results page
   * @param applicationConfig has been loaded from yml.
   * @return landing page predicate
   */
  private Predicate<Page> createGoogleSearchResultsPagePredicate(ApplicationConfig applicationConfig){
    String landingPageName = applicationConfig.getPage().getLandingPageName();
    return page -> landingPageName.equals(page.getPageName());
  }
}