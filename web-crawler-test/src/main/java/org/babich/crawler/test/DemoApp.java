package org.babich.crawler.test;

import static java.lang.System.exit;
import static java.lang.System.out;

import com.google.common.eventbus.Subscribe;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.babich.crawler.WebCrawler;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.messages.CrawlerStarted;
import org.babich.crawler.api.messages.CrawlerStopped;
import org.babich.crawler.api.messages.PageProcessingComplete;
import org.babich.crawler.configuration.ApplicationConfig;
import org.babich.crawler.configuration.exception.CrawlerConfigurationException;
import org.babich.crawler.configuration.processing.CustomPageProcessingConfig;
import org.babich.crawler.processing.DefaultJsoupPageProcessing;
import org.jsoup.nodes.Element;


public class DemoApp {

  private static final String USAGE_STRING = "java -jar web-crawler-test-{version}.jar [-help]"
      + " [-outputFile] [-searchStr] [-configPath]";

  private static final Options options;

  static {
    options = new Options();

    options.addOption(Option.builder("help")
        .required(false)
        .hasArg(false)
        .desc("print this message")
        .build());

    options.addOption(Option.builder("outputFile")
        .required(false)
        .hasArg(true)
        .desc("File path for the result.")
        .build()
    );

    options.addOption(Option.builder("searchStr")
        .required(false)
        .hasArg(true)
        .desc("Search string for google.")
        .build()
    );

    options.addOption(Option.builder("configPath")
        .required(false)
        .hasArg(true)
        .desc("Path to yml file of crawler configuration.")
        .build()
    );
  }

  public final String outputFile;
  public final String searchString;
  private final Path configurationPath;
  private PrintStream printStream;

  public static void main(String[] args) {
    try{
      DemoApp crawler = initApp(args);

      run(crawler);

      exit(0);
    } catch (Exception e){
      printUsage(e.getMessage());
    }
  }

  private static DemoApp initApp(String... args) {
    out.println("initializing..");

    CommandLine commandLine;
    try {
      commandLine = new DefaultParser().parse(options, args);
    } catch (ParseException e) {
      throw new IllegalArgumentException(e.getMessage());
    }

    if (commandLine.hasOption("help")) {
      printUsage();
      exit(0);
    }

    return new DemoApp(getConfigurationPath(commandLine)
        , getOutputFile(commandLine)
        , getSearchStr(commandLine));
  }

  private static String getSearchStr(CommandLine commandLine){
    return commandLine.getOptionValue("searchStr", "web+crawler");
  }

  private static String getOutputFile(CommandLine commandLine) {
     return commandLine.getOptionValue("outputFile");
  }

  private static String getConfigurationPath(CommandLine commandLine) {
    return commandLine.getOptionValue("configPath");
  }
  
  private static void run(DemoApp crawler){
    try {
      crawler.findWikiPageAndGrabTitle(crawler.searchString);
    } catch (CrawlerConfigurationException e) {
      e.printStackTrace();
    }
  }

  private static void printUsage() {
    System.out.println();

    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp(120, USAGE_STRING, "", options, "");
    out.println();
  }

  private static void printUsage(String message){
    out.println();
    out.println(message);
    printUsage();
  }

  public DemoApp(String outputFile, String searchString) {
    this(null, outputFile, searchString);
  }

  public DemoApp(String configurationPath,  String outputFile, String searchString) {
    this.configurationPath = null == configurationPath
        ? null : Paths.get(configurationPath);
    this.outputFile = outputFile;
    this.searchString = searchString;
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
   * the subscriber method is called immediately when the crawler starts
   * @param crawlerStarted contains configuration information.
   */
  @Subscribe
  public void OnCrawlerStarted(CrawlerStarted crawlerStarted) {

      if (StringUtils.isBlank(this.outputFile)) {
        printStream = System.out;
      }

      Path path = Paths.get(this.outputFile)
          .normalize()
          .toAbsolutePath();

      out.println(path.toFile().toURI().toString());

      try {
        printStream = new PrintStream(new FileOutputStream(path.toFile()));
      } catch (FileNotFoundException e) {
        out.println("The output file cannot be created, the console will be used for output."
            + e.getMessage());
        printStream = out;
      }
  }

  @Subscribe
  public void OnCrawlerStopped(CrawlerStopped crawlerStopped){
    try{
      if(null == printStream){
        return;
      }

      printStream.close();
    }catch (Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Collecting titles on all wiki pages
   * @param searchStr the string to be used by Google as the search string.
   */
  public void findWikiPageAndGrabTitle(String searchStr) throws CrawlerConfigurationException {

    //configuring crawler
    WebCrawler crawler = new WebCrawler.WebCrawlerBuilder("google", configurationPath)
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
