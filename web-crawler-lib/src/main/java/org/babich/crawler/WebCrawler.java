/*
 * @author Vadim Babich
 */
package org.babich.crawler;

import com.google.common.graph.Traverser;
import com.google.common.reflect.Reflection;
import com.google.common.util.concurrent.MoreExecutors;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageContext;
import org.babich.crawler.api.PageProcessing;
import org.babich.crawler.api.PageProcessingInterceptor;
import org.babich.crawler.api.messages.CrawlerStarted;
import org.babich.crawler.api.messages.CrawlerStopped;
import org.babich.crawler.api.processing.AssignedPageFilter;
import org.babich.crawler.api.processing.AssignedPagePostProcessing;
import org.babich.crawler.api.processing.AssignedPageProcessing;
import org.babich.crawler.configuration.ApplicationConfig;
import org.babich.crawler.configuration.ApplicationConfig.Processing;
import org.babich.crawler.configuration.ApplicationConfig.Traverser.Mode;
import org.babich.crawler.configuration.SelectiveConstructor;
import org.babich.crawler.configuration.exception.CrawlerConfigurationException;
import org.babich.crawler.configuration.processing.CustomPageProcessingConfig;
import org.babich.crawler.configuration.processing.CustomPageProcessingConfig.Builder;
import org.babich.crawler.configuration.processing.CustomProcessingFilter;
import org.babich.crawler.event.LocalEventBus;
import org.babich.crawler.exporters.S3PageSourceExporter;
import org.babich.crawler.interceptor.CustomMessagesDispatcher;
import org.babich.crawler.interceptor.DefaultMessageProducer;
import org.babich.crawler.interceptor.filter.PageFilterCombiner;
import org.babich.crawler.interceptor.service.SuccessorPagesPostProcessing;
import org.babich.crawler.metrics.InfluxRegistry;
import org.babich.crawler.processing.CombinePageProcessing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


@SuppressWarnings("UnstableApiUsage")
public class WebCrawler {

    protected static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);

    private final ApplicationConfig applicationConfig;

    private final LocalEventBus eventBus;

    private final Traverser<Page> traverser;

    private final String name;
    private final String landingUrl;

    private Consumer<Page> customPageConsumer;
    private CustomMessagesDispatcher<AssignedPagePostProcessing> customMessagesProducer;
    private AssignedPageProcessing[] customPageProcessors;
    private PageFilterCombiner pageFilterCombiner;

    private final AtomicBoolean isActive = new AtomicBoolean();

    private WebCrawler(String name, String lendingPage, ApplicationConfig applicationConfig) {
        this.name = name;
        this.landingUrl = lendingPage;
        this.applicationConfig = applicationConfig;
        this.eventBus = applicationConfig.getEventBus();
        this.traverser = Traverser.forTree(Page::getSuccessorPages);
    }

    public void start() {
        start(null);
    }

    public void start(Executor executor) {
        executor = null == executor ? MoreExecutors.newDirectExecutorService() : executor;

        if (!isActive.compareAndSet(false, true)) {
            throw new IllegalStateException("The " + name + " Crawler is already running.");
        }
        executor.execute(preparePageContext().apply(customPageConsumer));
    }

    public boolean isActive() {
        return isActive.get();
    }


    static ApplicationConfig loadYmlConfiguration(Path configurationPath) throws CrawlerConfigurationException {

        String[] packages = Stream.of(CombinePageProcessing.class, PageFilterCombiner.class, S3PageSourceExporter.class,
                SuccessorPagesPostProcessing.class, CustomMessagesDispatcher.class, InfluxRegistry.class)
                .map(Reflection::getPackageName)
                .toArray(String[]::new);

        return SelectiveConstructor.loadYmlConfiguration(configurationPath
                , ApplicationConfig.class
                , packages
                , LocalEventBus.class);
    }

    void setCustomerPageFilters(AssignedPageFilter... customPageFilters) {
        if (null == customPageFilters || customPageFilters.length == 0) {
            return;
        }

        pageFilterCombiner = new PageFilterCombiner(applicationConfig.getEventBus()
                , arrayAsStream(customPageFilters).collect(Collectors.toSet()));
    }

    void setCustomerPageProcessors(AssignedPageProcessing[] customPageProcessors) {
        if (null == customPageProcessors) {
            return;
        }
        this.customPageProcessors = customPageProcessors;
    }

    void setCustomerMessagesProducers(AssignedPagePostProcessing[] customerMessagesProducers) {
        if (null == customerMessagesProducers) {
            return;
        }

        this.customMessagesProducer = new CustomMessagesDispatcher<>(eventBus
                , new DefaultMessageProducer(eventBus)
                , customerMessagesProducers);
    }

    void setCustomPageConsumer(Consumer<Page> customPageConsumer) {
        this.customPageConsumer = null == customPageConsumer ? page -> {
        } : customPageConsumer;
    }

    private Collection<Page> getStartPages(PageContext pageContext) {
        Collection<Page> startPages = applicationConfig.getBackupService().restoreFor(pageContext);
        if (!startPages.isEmpty()) {
            return startPages;
        }

        return Collections.singleton(new Page(new AtomicReference<>(pageContext)
                , name
                , landingUrl
                , applicationConfig.getPage().getLandingPageName()));
    }

    private Iterable<Page> preparePageIterator(PageContext pageContext) {
        Collection<Page> startPages = getStartPages(pageContext);
        Mode mode = applicationConfig.getTraverser().getMode();
        switch (mode) {
            case DEPTH:
                return traverser.depthFirstPostOrder(startPages);
            case BREADTH:
                return traverser.breadthFirst(startPages);
            default:
                throw new IllegalArgumentException(String.format("Unknown traversal mode {%s}.", mode));
        }
    }

    private void registerEventListeners() {
        collectionAsStream(applicationConfig.getEventListeners()).forEach(eventBus::register);
    }

    private Function<Consumer<Page>, Runnable> preparePageContext() {
        PageContext.Builder pageContextBuilder = new PageContext.Builder()
                .pagesProcessed(0)
                .pageCount(0);

        registerPageProcessors(pageContextBuilder);
        registerPagePreProcessors(pageContextBuilder);
        registerEventListeners();

        PageContext context = pageContextBuilder.build();

        return pageConsumer -> () -> {
            eventBus.post(new CrawlerStarted(name));
            try {
                startAsStream(preparePageIterator(context), pageConsumer);
                eventBus.post(new CrawlerStopped(name));
            } catch (Throwable e) {
                eventBus.post(new CrawlerStopped(name, true));
                throw e;
            } finally {
                isActive.set(false);
            }
        };
    }

    void startAsStream(Iterable<Page> pageIterator, Consumer<Page> pageConsumer) {
        pageConsumer = null == pageConsumer ? page -> {
        } : pageConsumer;

        StreamSupport.stream(pageIterator.spliterator(), false)
                .limit(applicationConfig.getLimit().getCount())
                .map(Page::new)
                .forEach(pageConsumer);
    }

    void registerPageProcessors(PageContext.Builder builder) {
        Processing processing = applicationConfig.getProcessing();

        AssignedPageProcessing[] pageProcessors = Stream.concat(
                arrayAsStream(customPageProcessors),
                collectionAsStream(processing.getProcessingList()))
                .toArray(AssignedPageProcessing[]::new);

        builder.pageProcessing(new CombinePageProcessing(processing.getDefaultProcessing(), pageProcessors));

        if (0 == pageProcessors.length) {
            logger.debug("No page processors configured.");
            return;
        }

        logger.debug("{} pager processors registered", pageProcessors.length);
    }

    void registerPagePreProcessors(PageContext.Builder builder) {

        PageProcessingInterceptor[] preProcessingStream = Stream.concat(
                arrayAsStream(customMessagesProducer, pageFilterCombiner),
                collectionAsStream(applicationConfig.getInterceptorList()))
                .toArray(PageProcessingInterceptor[]::new);

        if (0 == preProcessingStream.length) {
            logger.debug("No page interceptors configured.");
            return;
        }

        builder.interceptors(preProcessingStream);

        logger.debug("{} pager preprocessors registered", preProcessingStream.length);
    }

    void registerEventListeners(Set<Object> customListeners) {
        if (null == eventBus) {
            logger.info("The crawler was started with no eventbus configured. Messages not available.");
            return;
        }

        customListeners = Stream.concat(
                collectionAsStream(applicationConfig.getEventListeners()),
                collectionAsStream(customListeners))
                .collect(Collectors.toSet());

        if (customListeners.isEmpty()) {
            logger.info("No event listeners configured. Messages not available.");
            return;
        }

        customListeners.forEach(eventBus::register);

        logger.debug("{} event listeners registered", customListeners.size());
    }

    private static <T> Stream<T> collectionAsStream(Collection<T> collection) {
        return Optional.ofNullable(collection).map(Collection::stream).orElse(Stream.empty());
    }

    @SuppressWarnings("unchecked")
    private static <T> Stream<T> arrayAsStream(T... array) {
        return Optional.ofNullable(array).map(Arrays::stream).orElse(Stream.empty())
                .filter(Objects::nonNull);
    }

    public static class WebCrawlerBuilder {

        private final String name;

        private String startUrl;

        private Integer maxDepth;

        private Consumer<Page> pageConsumer;

        private Set<Object> eventListeners;

        private ApplicationConfig config;

        private ApplicationConfig.Traverser.Mode mode;

        private final List<CustomPageProcessingConfig> processingConfigList = new LinkedList<>();

        private PageProcessing defaultPageProcessing;

        public WebCrawlerBuilder() throws CrawlerConfigurationException {
            this("default", null);
        }

        public WebCrawlerBuilder(String name) throws CrawlerConfigurationException {
            this(name, null);
        }

        public WebCrawlerBuilder(String name, Path configurationPath)
            throws CrawlerConfigurationException {
            this.name = name;
            this.config = loadYmlConfiguration(configurationPath);
        }

        public WebCrawlerBuilder pageConsumer(Consumer<Page> pageConsumer) {
            this.pageConsumer = null == pageConsumer ? page -> {
            } : pageConsumer;
            return this;
        }

        public WebCrawlerBuilder withCustomPageProcessing(Consumer<Builder> builder) {
            Builder configBuilder = new Builder(config);
            builder.accept(configBuilder);

            processingConfigList.add(configBuilder.build());
            return this;
        }

        public WebCrawlerBuilder eventListeners(Object... listeners) {
            if (null == listeners || listeners.length == 0) {
                return this;
            }

            eventListeners = Arrays.stream(listeners).collect(Collectors.toSet());
            return this;
        }

        public WebCrawlerBuilder startUrl(String startUrl) {
            this.startUrl = startUrl;
            return this;
        }

        public WebCrawlerBuilder maxDepth(Integer maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public WebCrawlerBuilder traverserMode(ApplicationConfig.Traverser.Mode mode) {
            this.mode = mode;
            return this;
        }

        public WebCrawlerBuilder useDefaultPageProcessing(PageProcessing defaultPageProcessing) {
            this.defaultPageProcessing = defaultPageProcessing;
            return this;
        }

        public WebCrawler build() {

            WebCrawler crawler = new WebCrawler(name, startUrl, config);

            setMaxDepth(config);
            setTraversalMode(config);
            setDefaultPageProcessing(config);

            crawler.setCustomPageConsumer(pageConsumer);
            crawler.registerEventListeners(eventListeners);
            crawler.setCustomerPageProcessors(getCustomPageProcessors());
            crawler.setCustomerPageFilters(getCustomPageProcessingFilter());
            crawler.setCustomerMessagesProducers(getCustomMessagesProducers());

            return crawler;
        }

        private void setMaxDepth(ApplicationConfig config) {
            Optional.ofNullable(maxDepth)
                    .ifPresent(value -> config.getLimit().setMaxDepth(value));
        }

        private void setTraversalMode(ApplicationConfig config) {
            Optional.ofNullable(mode)
                    .ifPresent(value -> config.getTraverser().setMode(value));
        }

        private void setDefaultPageProcessing(ApplicationConfig config) {
            Optional.ofNullable(defaultPageProcessing)
                    .ifPresent(value -> config.getProcessing().setDefaultProcessing(value));
        }

        private AssignedPageFilter[] getCustomPageProcessingFilter() {
            return processingConfigList.stream()
                    .map(pp -> AssignedPageFilter.of(pp.getPredicate()
                            , Optional.ofNullable(pp.getFilter()).map(CustomProcessingFilter::getPredicate)
                                    .orElse(filter -> false)
                            , Optional.ofNullable(pp.getFilter()).map(CustomProcessingFilter::getMessageFactory)
                                    .orElse(null)
                    ))
                    .toArray(AssignedPageFilter[]::new);
        }

        private AssignedPagePostProcessing[] getCustomMessagesProducers() {
            return processingConfigList.stream()
                    .map(pp -> AssignedPagePostProcessing.of(pp.getPredicate(), pp.getMessageProducer()))
                    .toArray(AssignedPagePostProcessing[]::new);
        }

        private AssignedPageProcessing[] getCustomPageProcessors() {
            return processingConfigList.stream()
                    .filter(pp -> Objects.nonNull(pp.getPageProcessing()))
                    .map(pp -> AssignedPageProcessing.of(pp.getPredicate(), pp.getPageProcessing()))
                    .toArray(AssignedPageProcessing[]::new);
        }

    }
}
