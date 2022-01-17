/*
 * @author Vadim Babich
 */
package org.babich.crawler.api;

import java.io.Serializable;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.StringUtils;

/**
 * Each URL must be represented as an object of this class.
 * The class is serializable so an object of that class can be used for communication between components.
 */
public class Page implements Serializable{
    private static final long serialVersionUID = 1;

    //end-to-end context that is passed from one page to another on each load.
    private final AtomicReference<PageContext> pageContextRef;
    //successors found when processing this page
    private transient Iterable<Page> successorPages;

    //HTML of the page as a string
    private String pageSource;
    //the name of the Crawler that can be used in message consumers
    private String crawlerName;
    //the page url used for loading
    private String pageUrl;
    //this is a synthetic page name. It is used to represent the page in messages and logs.
    private String pageName;
    //this is the delay that was applied before the page was loaded by the {@code PageProcessingDelay} service.
    private int delay;
    //this is the depth of the loading graph from the landing page to the current one
    private int depth;
    //data size of the page source
    private long size;
    //any serializable data that can be assigned to this page
    private Serializable payload;


    public Page(AtomicReference<PageContext> pageContextRef, String crawlerName, String pageUrl, String name) {
        this(pageContextRef, crawlerName, pageUrl, name, StringUtils.EMPTY, 0, 0, 0, null);
    }

    public Page(AtomicReference<PageContext> pageContextRef, String crawlerName, String pageUrl, String name,
            String pageSource,
            int delay,
            int depth,
            long size,
            Serializable payload) {
        this.pageContextRef = pageContextRef;
        this.crawlerName = crawlerName;
        this.pageUrl = pageUrl;
        this.pageSource = pageSource;
        this.delay = delay;
        this.pageName = name;
        this.depth = depth;
        this.size = size;
        this.payload = payload;
    }

    public Page(Page page){
        this.pageContextRef = new AtomicReference<>(new PageContext(page.getPageContextRef().get()));
        this.crawlerName = page.crawlerName;
        this.pageUrl = page.pageUrl;
        this.pageName = page.pageName;
        this.pageSource = page.pageSource;
        this.delay = page.delay;
        this.depth = page.depth;
        this.size = page.size;
        this.payload = page.payload;
    }

    /**
     * This is the contract used by page TreeTraverser.
     * Using {@code PageProcessing} from the end-to-end context to process the current page.
     * @return collection of successors found when processing this page.
     */
    public Iterable<Page> getSuccessorPages() {

        if(getPageContextRef().get().getPageProcessing() == null){
            throw new UnsupportedOperationException("The page has detached the context.");
        }

        if(null != this.successorPages){
            return this.successorPages;
        }

        this.successorPages = pageContextRef.get()
                .getPageProcessing()
                .process(this);

        return successorPages;
    }

    public String getCrawlerName() {
        return crawlerName;
    }

    public void setCrawlerName(String crawlerName) {
        this.crawlerName = crawlerName;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getPageSource() {
        return pageSource;
    }

    public void setPageSource(String pageSource) {
        this.pageSource = pageSource;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public AtomicReference<PageContext> getPageContextRef() {
        return pageContextRef;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public Serializable getPayload() {
        return payload;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setPayload(Serializable payload) {
        this.payload = payload;
    }


    @Override
    public String toString() {
        return new StringJoiner(", ", Page.class.getSimpleName() + "[", "]")
                .add("pageContextRef=" + pageContextRef)
                .add("crawlerName='" + crawlerName + "'")
                .add("pageUrl='" + pageUrl + "'")
                .add("pageSource='" + StringUtils.length(pageSource) + "'")
                .add("pageName='" + pageName + "'")
                .add("delay=" + delay)
                .add("depth=" + depth)
                .toString();
    }

}
