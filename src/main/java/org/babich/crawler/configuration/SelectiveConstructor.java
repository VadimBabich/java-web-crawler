/*
 * @author Vadim Babich
 */
package org.babich.crawler.configuration;

import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.common.reflect.Reflection;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.babich.crawler.configuration.exception.CrawlerConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.representer.Representer;

/**
 * This class is an extension to the yaml-engine and provides an approach to initializing an object of configuration
 * {@code classes} using the most matching from accessible constructor.
 */
public class SelectiveConstructor extends Constructor {

    private static final Logger logger = LoggerFactory.getLogger(SelectiveConstructor.class);

    public SelectiveConstructor(String[] packageNames, Class<?>... classes) {

        Predicate<Class<?>> predicate = aClass -> false;
        if (!(null == packageNames || packageNames.length == 0)) {
            predicate = predicate.or(getPackageNamesPredicate(packageNames));
        }

        if (!(null == classes || classes.length == 0)) {
            predicate = predicate.or(getClassMatchedPredicate(classes));
        }

        yamlClassConstructors.put(
                NodeId.mapping, new MatchingAccessibleConstructor(predicate));

    }

    class MatchingAccessibleConstructor extends ConstructMapping {

        private final Predicate<Class<?>> predicate;

        public MatchingAccessibleConstructor(Predicate<Class<?>> predicate) {
            this.predicate = predicate;
        }

        @Override
        protected Object constructJavaBean2ndStep(MappingNode node, Object object) {
            Class<?> nodeType = node.getType();
            if (predicate.negate().test(nodeType)) {
                return super.constructJavaBean2ndStep(node, object);
            }

            Map<Object, Object> arguments = constructMapping(node);
            try {
                return newInstanceBy(nodeType, arguments);
            } catch (ReflectiveOperationException exception) {
                logger.error("Unable to parse yml config node {}"
                        , node);
                return super.constructJavaBean2ndStep(node, object);
            }
        }
    }

    public static <T> T newInstanceBy(Class<T> type, Map<Object, Object> arguments)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        Class<?>[] argTypes = arguments.values().stream()
                .map(Object::getClass)
                .toArray(Class[]::new);

        Object[] args = arguments.values().toArray();

        java.lang.reflect.Constructor<T> constructor =
                ConstructorUtils.getMatchingAccessibleConstructor(type, argTypes);
        if (null == constructor) {
            throw new NoSuchMethodException("No such accessible constructor on object: " + type.getName());
        }
        return constructor.newInstance(args);
    }

    static Predicate<Class<?>> getClassMatchedPredicate(Class<?>[] classes) {
        return aClass -> {
            for (Class<?> cls : classes) {
                if (cls.isAssignableFrom(aClass)) {
                    return true;
                }
            }
            return false;
        };
    }

    @SuppressWarnings("UnstableApiUsage")
    static Predicate<Class<?>> getPackageNamesPredicate(String[] packageNames) {
        Set<String> packageNameSet = Sets.newHashSet(packageNames);
        return aClass -> packageNameSet.contains(Reflection.getPackageName(aClass));
    }

    public static <T> T loadYmlConfiguration(Path configurationPath
            , Class<T> configurationClass
            , String[] packages
            , Class<?> ...classes) throws CrawlerConfigurationException {

        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowRecursiveKeys(true);

        Yaml yaml = new Yaml(new SelectiveConstructor(packages, classes)
                , new Representer()
                , new DumperOptions()
                , loaderOptions);

        T config;
        try {
            URL url = null == configurationPath ? Resources.getResource("crawler.yml")
                    : configurationPath.toUri().toURL();

            logger.info("Crawler is launched with configuration from resource {}", url);
            config = yaml.loadAs(Resources.toString(url, Charset.defaultCharset()), configurationClass);
            logger.debug("Loaded configuration {}", config);

        } catch (IOException e) {
            throw new CrawlerConfigurationException(
                    String.format("Exception occurred when loading crawler configuration from {%s}: "
                            , configurationPath), e);
        }

        if (null == config) {
            throw new CrawlerConfigurationException(String.format("Cannot load configuration from {%s}: "
                    , configurationPath));
        }

        return config;
    }
}
