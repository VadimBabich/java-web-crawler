/*
 * @author Vadim Babich
 */
package org.babich.crawler.interceptor;

import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessingInterceptor;
import org.babich.crawler.api.messages.SuccessorsProcessingComplete;
import org.babich.crawler.api.messages.PageRecovered;
import org.babich.crawler.event.LocalEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class SuccessorsMessageProducer implements PageProcessingInterceptor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Set<String> passedLinks = Sets.newConcurrentHashSet();
    private final MutableValueGraph<String, Page> graph = ValueGraphBuilder
            .directed()
            .allowsSelfLoops(true)
            .build();

    private LocalEventBus eventBus;

    private SuccessorsMessageProducer() {
    }

    public SuccessorsMessageProducer(LocalEventBus eventBus) {
        this.eventBus = eventBus;
    }


    @Override
    public void afterProcessing(Page page, List<Page> successorPages) {

        passedLinks.add(page.getPageUrl());

        Consumer<Page> newNode = p -> {
            synchronized (graph) {
                graph.addNode(p.getPageUrl());
                graph.putEdgeValue(page.getPageUrl(), p.getPageUrl(), page);
            }
        };
        successorPages.forEach(newNode);

        Consumer<String> sentMessage = parent -> graph.edgeValue(parent, page.getPageUrl()).ifPresent(p -> {
            if (null != eventBus) {
                eventBus.post(new SuccessorsProcessingComplete(new Page(p)));
            }
            logger.debug("Each successor for the page {} has been loaded.", p);
        });

        synchronized (graph) {
            graph.predecessors(page.getPageUrl())
                    .stream().filter(parent -> passedLinks.containsAll(graph.adjacentNodes(parent)))
                    .forEach(sentMessage);
        }
    }

    @Subscribe
    public void pageOnRecovered(PageRecovered message) {
        synchronized (graph) {
            graph.addNode(message.getPage().getPageUrl());
        }
    }

}
