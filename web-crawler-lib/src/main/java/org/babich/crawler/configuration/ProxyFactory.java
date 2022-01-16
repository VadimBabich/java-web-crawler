/*
 * @author Vadim Babich
 */
package org.babich.crawler.configuration;

import com.google.common.collect.Lists;
import com.google.common.reflect.Reflection;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessing;
import org.babich.crawler.api.PageProcessingInterceptor;
import org.babich.crawler.configuration.exception.PreProcessingChainException;
import org.babich.crawler.metrics.PageProcessingServiceMetricsProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class configures the preprocessing hooks for the {@link PageProcessingInterceptor} interface.
 */
public class ProxyFactory {

    private ProxyFactory() {
    }

    @SuppressWarnings("UnstableApiUsage")
    public static PageProcessing configureProcessingProxy(final PageProcessing delegate
            , List<PageProcessingInterceptor> interceptorList) {

        Method processingMethod = getProcessingMethod();

        CombinePageProcessingInterceptor interceptor = new CombinePageProcessingInterceptor(interceptorList);

        ProcessingMethodInvocationHandler invocationHandler =
                new ProcessingMethodInvocationHandler(delegate
                        , processingMethod::equals
                        , interceptor);

        return Reflection.newProxy(PageProcessing.class, invocationHandler);
    }

    private static Method getProcessingMethod() {
        String methodName = "process";
        try {
            return PageProcessing.class.getDeclaredMethod(methodName, Page.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(String.format("Proxied method {%s} with params {%s} was not found."
                    , methodName, Page.class.getCanonicalName()));
        }
    }

    /**
     * Combine into a preprocessing execution chain and sort it in order. The minimum value is executed first.
     */
    private static class CombinePageProcessingInterceptor implements PageProcessingInterceptor {

        private final List<PageProcessingInterceptor> interceptorsChain;

        public CombinePageProcessingInterceptor(List<PageProcessingInterceptor> interceptorsChain) {

            Comparator<PageProcessingInterceptor> byOrder = Comparator
                    .comparingInt(PageProcessingInterceptor::getOrder);

            this.interceptorsChain = interceptorsChain.stream()
                    .sorted(byOrder)
                    .map(PageProcessingServiceMetricsProducer::new)
                    .collect(Collectors.toCollection(Lists::newCopyOnWriteArrayList));
        }

        @Override
        public void beforeProcessing(Page page) {
            interceptorsChain.forEach(item -> item.beforeProcessing(page));
        }

        @Override
        public void afterProcessing(Page page, List<Page> successorPages) {
            interceptorsChain.forEach(item -> item.afterProcessing(page, successorPages));
        }
    }

    private static class ProcessingMethodInvocationHandler implements InvocationHandler, PageProcessingInterceptor {

        final Logger logger = LoggerFactory.getLogger(this.getClass());

        private final Object delegate;
        private final Predicate<Method> methodPredicate;
        private final PageProcessingInterceptor interceptor;

        public ProcessingMethodInvocationHandler(Object delegate,
                                                 Predicate<Method> methodPredicate, PageProcessingInterceptor interceptor) {
            if (null == delegate) {
                throw new IllegalArgumentException("delegate cannot be null.");
            }

            if (null == methodPredicate) {
                throw new IllegalArgumentException("methodPredicate cannot be null.");
            }

            if (null == interceptor) {
                throw new IllegalArgumentException("interceptor cannot be null.");
            }

            this.delegate = delegate;
            this.methodPredicate = methodPredicate;
            this.interceptor = interceptor;
        }

        /**
         * Execution pre processing phase and implementation of the function to prevent page processing.
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                if (methodPredicate.test(method)) {
                    beforeProcessing((Page) args[0]);
                }

                Object value = method.invoke(delegate, args);

                if (methodPredicate.test(method)) {
                    afterProcessing((Page) args[0], (List<Page>) value);
                }

                return value;
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            } catch (PreProcessingChainException e) {

                Page page = ((Page) args[0]);
                logger.debug("Processing of a page {} has been interrupted for reason {}"
                        , page.getPageName()
                        , e.getMessage());

                return Collections.emptyList();
            }
        }

        @Override
        public void beforeProcessing(Page page) {
            interceptor.beforeProcessing(page);
        }

        @Override
        public void afterProcessing(Page page, List<Page> successorPages) {
            interceptor.afterProcessing(page, successorPages);
        }

    }

}
