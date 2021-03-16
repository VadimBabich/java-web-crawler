package org.babich.crawler.interceptor.service;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageProcessingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * URI normalization is the process by which URIs are modified and standardized in a consistent manner.
 * The goal of the normalization process is to transform a URI into a normalized URI so it is possible to determine
 * if two syntactically different URIs may be equivalent.
 * <p/>Supported
 * <ul>
 *     <li><p> Converting the scheme and host to lowercase.</p></li>
 *     <li><p> Removing dot-segments.</p></li>
 *     <li><p> Removing the default port.</p></li>
 *     <li><p> Removing the fragment.</p></li>
 *     <li><p> Removing duplicate slashes.</p></li>
 *     <li><p>Removing the "?" when the query is empty.</p></li>
 *     <li><p> Sorting the query parameters.</p></li>
 * </ul>
 */
public class PageUrlNormalizer implements PageProcessingInterceptor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    static final String regexUri = "^((?<scheme>[^:/?#]+):)" +
            "?(//(?<authority>(((?<user>[^@]+)@)?(?<host>[A-Za-z0-9.\\-_~]+)(:(?<port>\\d*))?)))" +
            "?(?<path>[^?#]*)" +
            "(\\?(?<query>[^#]*))" +
            "?(#(?<fragment>.*))" +
            "?(?i)";

    static final String queryParamsSeparator = "&";
    static final Pattern pattern = Pattern.compile(regexUri);
    static final Pattern queryPattern = Pattern.compile(queryParamsSeparator);

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void afterProcessing(Page page, List<Page> successorPages) {
        successorPages.forEach(this::normalizeUrl);
    }

    @Override
    public void beforeProcessing(Page page) {
        normalizeUrl(page);
    }

    protected void normalizeUrl(Page page)  {
        String pageUrl = page.getPageUrl();
        if (Strings.isNullOrEmpty(pageUrl)) {
            return;
        }

        try {
            URI uri = new URI(pageUrl);
            page.setPageUrl(normalize(uri));
        } catch (URISyntaxException e) {
            logger.error("Cannot parse URL {} string for te page {}."
                    , pageUrl, page.getPageName(), e);
        }
    }

    protected String normalize(URI uri){
        uri = uri.normalize();
        String uriAsString = uri.toString();
        Matcher matcher = pattern.matcher(uriAsString);

        if (!matcher.find()) {
            return uriAsString;
        }

        StringBuilder sourceUriString = new StringBuilder(uriAsString);

        convertingSchemeAndHostToLowercase(matcher, sourceUriString);
        sortingQueryParameters(uri, matcher, sourceUriString);

        removingFragment(matcher, sourceUriString);
        removingQueryMarkWhenQueryIsEmpty(matcher, sourceUriString);
        removingDefaultPort(matcher, sourceUriString);

        return sourceUriString.toString();
    }

    private void sortingQueryParameters(URI uri, Matcher matcher, StringBuilder source) {
        String queryGroupName = "query";
        Consumer<String> sortQueryParams = queryString ->
        {
            String sortedQueryParams = queryPattern.splitAsStream(queryString)
                    .sorted()
                    .collect(Collectors.joining(queryParamsSeparator));

            source.replace(matcher.start(queryGroupName), matcher.end(queryGroupName), sortedQueryParams);
        };

        Optional.ofNullable(uri.getQuery()).ifPresent(sortQueryParams);
    }

    private void removingFragment(Matcher matcher, StringBuilder source) {
        String fragmentGroupName = "fragment";
        String fragment = matcher.group(fragmentGroupName);
        if (StringUtils.isBlank(fragment)) {
            return;
        }

        source.delete(matcher.start(fragmentGroupName) - 1, matcher.end(fragmentGroupName));
    }

    private void removingDefaultPort(Matcher matcher, StringBuilder source) {
        String portGroupName = "port";
        String port = matcher.group(portGroupName);
        if (!"80".equals(port)) {
            return;
        }
        source.delete(matcher.start(portGroupName) - 1, matcher.end(portGroupName));
    }
    private void removingQueryMarkWhenQueryIsEmpty(Matcher matcher, StringBuilder source){
        if(StringUtils.isNotBlank(matcher.group("query"))){
            return;
        }

        int position;
        if(-1 == (position = matcher.end("path"))) {
            return;
        }

        if(position < source.length() && '?' == source.charAt(position)){
            source.delete(position, position + 1);
        }
    }

    private void convertingSchemeAndHostToLowercase(Matcher matcher, StringBuilder source) {
        replaceGroupToLowerCase(matcher, "scheme", source);
        replaceGroupToLowerCase(matcher, "host", source);
    }

    private static void replaceGroupToLowerCase(Matcher matcher, String groupName, StringBuilder source) {
        String value = matcher.group(groupName);
        if (StringUtils.isBlank(value)) {
            return;
        }
        source.replace(matcher.start(groupName), matcher.end(groupName), value.toLowerCase());
    }

}
