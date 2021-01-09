/*
 * @author Vadim Babich
 */
package org.babich.crawler.interceptor.service;

import java.util.Random;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessingInterceptor;
import org.babich.crawler.configuration.ApplicationConfig.Delay;

public class PageProcessingDelay implements PageProcessingInterceptor {

    private Delay delay;

    private PageProcessingDelay() {
    }

    public PageProcessingDelay(Delay delay) {
        if (delay.getMin() < 0 || delay.getMax() < delay.getMin()) {
            throw new IllegalArgumentException("Incorrect setting of page processing delay.");
        }
        this.delay = delay;
    }

    @Override
    public void beforeProcessing(Page page) {
        page.setDelay(await(delay.getMin(), delay.getMax()));
    }

    protected int await(int min, int maxTimeout) {
        int delayMs = min == maxTimeout ? min : new Random().ints(min, maxTimeout)
                .findFirst()
                .orElse(maxTimeout);

        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return delayMs;
    }
}
