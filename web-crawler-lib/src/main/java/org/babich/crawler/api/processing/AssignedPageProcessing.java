/*
 * @author Vadim Babich
 */
package org.babich.crawler.api.processing;

import java.util.function.Predicate;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessing;

public interface AssignedPageProcessing extends PageProcessing, Predicate<Page> {

    default boolean test(Page page){
        return matches(page);
    }

    boolean matches(Page page);

    static AssignedPageProcessing of(Predicate<Page> relevantPredicate
            , PageProcessing pageProcessing){

        return new AssignedPageProcessing() {
            @Override
            public Iterable<Page> process(Page page) {
                return pageProcessing.process(page);
            }

            @Override
            public boolean matches(Page page) {
                return relevantPredicate.test(page);
            }
        };
    }
}
