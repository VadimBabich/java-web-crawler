/*
 * @author Vadim Babich
 */
package org.babich.crawler.common;

import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestHelper {

    private TestHelper() {
    }

    /**
     * replacing ${@code (?<host>localhost)} host group value from {@code pattern} to {@code replacement} value
     */
    public static UnaryOperator<String> replaceUrl(Pattern pattern, String replacement) {
        return url -> {
            Matcher matcher = pattern.matcher(url);
            if (!matcher.find()) {
                return url;
            }
            return new StringBuilder(url)
                    .replace(matcher.start("host"), matcher.end("host"), replacement)
                    .toString();
        };
    }
}
