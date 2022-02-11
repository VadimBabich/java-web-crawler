/*
 * @author Vadim Babich
 */
package org.babich.crawler.configuration;


import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;

import io.micrometer.core.instrument.MeterRegistry;
import org.babich.crawler.api.BackupService;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessing;
import org.babich.crawler.api.PageProcessingInterceptor;
import org.babich.crawler.api.processing.AssignedPageProcessing;
import org.babich.crawler.event.LocalEventBus;

/**
 * crawler configuration
 */
public class ApplicationConfig {

    //page filters, if a filter is triggered, processing for the page is skipped
    //and the message must be sent to eventListeners.
    private List<Predicate<Page>> filters;

    //filters, services, message producers
    private List<PageProcessingInterceptor> interceptorList;

    //processing assigned to pages
    private Processing processing;

    //list of subscribers registered for the messages
    private List<?> eventListeners;

    //event bus used to send crawler messages
    private LocalEventBus eventBus;
    //this service provides an approach to backup and restore the state of the crawler in case of failure.
    private BackupService backupService;

    //page processing delay settings
    private Delay delay;
    private PageConfig page;
    //Processing restrictions
    private Limit limit;
    private Traverser traverser;

    private Metrics metrics = new Metrics();


    /**
     * This is a metrics configuration that provides the ability to override the registry of metrics.
     */
    public static class Metrics {
        private MeterRegistry registry;
        private Boolean enabled = true;

        public MeterRegistry getRegistry() {
            return registry;
        }

        public void setRegistry(MeterRegistry registry) {
            this.registry = registry;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Metrics.class.getSimpleName() + "[", "]")
                    .add("registry='" + registry + "'")
                    .add("enabled='" + enabled + "'")
                    .toString();
        }
    }

    /**
     * setting page attributes such as name
     */
    public static class PageConfig {

        private String landingPageName;
        private String pageNamePattern;
        private String userAgent;
        private Boolean preLoad;

        public String getLandingPageName() {
            return landingPageName;
        }

        public void setLandingPageName(String landingPageName) {
            this.landingPageName = landingPageName;
        }

        public String getPageNamePattern() {
            return pageNamePattern;
        }

        public void setPageNamePattern(String pageNamePattern) {
            this.pageNamePattern = pageNamePattern;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public Boolean getPreLoad() {
            return preLoad;
        }

        public void setPreLoad(Boolean preLoad) {
            this.preLoad = preLoad;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", PageConfig.class.getSimpleName() + "[", "]")
                    .add("landingPageName='" + landingPageName + "'")
                    .add("pageNamePattern='" + pageNamePattern + "'")
                    .add("userAgent='" + userAgent + "'")
                    .add("preLoad='" + preLoad + "'")
                    .toString();
        }
    }

    /**
     * page processing delay settings {@code min} and {@code max} delay in ms that applied before page processing.
     */
    public static class Delay {

        private int min;
        private int max;


        public int getMin() {
            return min;
        }

        public void setMin(int min) {
            this.min = min;
        }

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            this.max = max;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Delay.class.getSimpleName() + "[", "]")
                    .add("min=" + min)
                    .add("max=" + max)
                    .toString();
        }
    }

    /**
     * Processing restrictions such as the maximum number of pages to be processed, maximum depth from the landing page,
     * processing time, etc.
     */
    public static class Limit {

        private int count;
        private long duration;
        private int maxDepth;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public int getMaxDepth() {
            return maxDepth;
        }

        public void setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Limit.class.getSimpleName() + "[", "]")
                    .add("count=" + count)
                    .add("duration=" + duration)
                    .add("maxDepth=" + maxDepth)
                    .toString();
        }
    }

    /**
     * Traverser is used to traversing the pages loading and processing tree.
     */
    public static class Traverser {

        public enum Mode {
            DEPTH,
            BREADTH
        }

        private Mode mode;

        public Mode getMode() {
            return mode;
        }

        public void setMode(Mode mode) {
            this.mode = mode;
        }


    }

    /**
     * processing assigned to pages
     */
    public static class Processing {

        private PageProcessing defaultProcessing;
        private List<AssignedPageProcessing> processingList;


        public PageProcessing getDefaultProcessing() {
            return defaultProcessing;
        }

        public void setDefaultProcessing(PageProcessing defaultProcessing) {
            this.defaultProcessing = defaultProcessing;
        }

        public List<AssignedPageProcessing> getProcessingList() {
            return processingList;
        }

        public void setProcessingList(List<AssignedPageProcessing> processingList) {
            this.processingList = processingList;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Processing.class.getSimpleName() + "[", "]")
                    .add("defaultProcessing=" + defaultProcessing)
                    .add("processingList=" + processingList)
                    .toString();
        }
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public PageConfig getPage() {
        return page;
    }

    public void setPage(PageConfig page) {
        this.page = page;
    }

    public Delay getDelay() {
        return delay;
    }

    public void setDelay(Delay delay) {
        this.delay = delay;
    }

    public Processing getProcessing() {
        return processing;
    }

    public void setProcessing(Processing processing) {
        this.processing = processing;
    }

    public List<PageProcessingInterceptor> getInterceptorList() {
        return interceptorList;
    }

    public void setInterceptorList(List<PageProcessingInterceptor> interceptorList) {
        this.interceptorList = interceptorList;
    }

    public List<?> getEventListeners() {
        return eventListeners;
    }

    public void setEventListeners(List<?> eventListeners) {
        this.eventListeners = eventListeners;
    }

    public LocalEventBus getEventBus() {
        return eventBus;
    }

    public void setEventBus(LocalEventBus eventBus) {
        this.eventBus = eventBus;
    }

    public Limit getLimit() {
        return limit;
    }

    public void setLimit(Limit limit) {
        this.limit = limit;
    }

    public Traverser getTraverser() {
        return traverser;
    }

    public void setTraverser(Traverser traverser) {
        this.traverser = traverser;
    }

    public BackupService getBackupService() {
        return backupService;
    }

    public void setBackupService(BackupService backupService) {
        this.backupService = backupService;
    }

    public List<Predicate<Page>> getFilters() {
        return filters;
    }

    public void setFilters(List<Predicate<Page>> filters) {
        this.filters = filters;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ApplicationConfig.class.getSimpleName() + "[", "]")
                .add("processing=" + processing)
                .add("interceptorList=" + interceptorList)
                .add("eventListeners=" + eventListeners)
                .add("eventBus=" + eventBus)
                .add("timeout=" + delay)
                .add("page=" + page)
                .add("limit=" + limit)
                .add("traverser=" + traverser)
                .add("metrics=" + metrics)
                .toString();
    }
}
