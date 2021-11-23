/*
 * @author Vadim Babich
 */
package org.babich.crawler.common;

import com.google.common.eventbus.Subscribe;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.babich.crawler.api.messages.CrawlerStarted;
import org.babich.crawler.api.messages.CrawlerStopped;
import org.babich.crawler.api.messages.PageProcessingComplete;
import org.babich.crawler.api.messages.PageProcessingSkippe;
import org.babich.crawler.api.messages.SuccessorsProcessingComplete;

public class ProcessingMessageListener {

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
