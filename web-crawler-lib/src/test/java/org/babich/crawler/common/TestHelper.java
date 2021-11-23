/*
 * @author Vadim Babich
 */
package org.babich.crawler.common;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
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

    /**
     * Converting a URI, represented as a string, to a file
     * @param uriString URI represented as a string
     * @return file
     */
    public static File toFile(String uriString){
        try {
            return Paths.get(new URI(uriString)).toFile();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
