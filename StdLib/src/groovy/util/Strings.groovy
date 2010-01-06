package groovy.util

import java.util.regex.Pattern
import java.util.regex.Matcher

@Typed class Strings {

    /**
     * Process each regex group matched substring of the given string. If the closure
     * parameter takes one argument, an array with all match groups is passed to it.
     * If the closure takes as many arguments as there are match groups, then each
     * parameter will be one match group.
     *
     * @param self    the source string
     * @param regex   a Regex string
     * @param closure a closure with one parameter
     * @return the source string
     */
    static String eachMatch(CharSequence self, String regex, Function1<?,?> closure) {
        eachMatch(self, Pattern.compile(regex), closure)
    }

    /**
     * Process each regex group matched substring of the given pattern. If the closure
     * parameter takes one argument, an array with all match groups is passed to it.
     * If the closure takes as many arguments as there are match groups, then each
     * parameter will be one match group.
     *
     * @param self    the source string
     * @param pattern a regex Pattern
     * @param closure a closure with one parameter
     * @return the source string
     */
    static String eachMatch(CharSequence self, Pattern pattern, Function1<?,?> closure) {
        pattern.matcher(self).iterator().each(closure)
        self
    }
}