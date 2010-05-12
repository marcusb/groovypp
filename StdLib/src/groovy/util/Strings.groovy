/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package groovy.util

import java.util.regex.Matcher
import java.util.regex.Pattern
import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport
import org.codehaus.groovy.runtime.DefaultGroovyMethods

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
    static String eachMatch(CharSequence self, Matcher pattern, Function1<?,?> closure) {
        pattern.reset(self).iterator().each(closure)
        self
    }

    // Strings

    /**
     * Iterates through this String line by line.  Each line is passed
     * to the given 1 arg closure.
     *
     * @param self a String
     * @param closure a closure
     * @return the last value returned by the closure
     * @throws java.io.IOException if an error occurs
     * @see #eachLine(java.lang.String, int, groovy.lang.Function1)
     */
    static Object eachLine(String self, Function1<String, ?> closure) {
        eachLine(self, 0, closure)
    }

    /**
     * Iterates through this String line by line.  Each line is passed
     * to the given 2 arg closure. The line count is passed as the second argument.
     *
     * @param self a String
     * @param closure a closure
     * @return the last value returned by the closure
     * @throws java.io.IOException if an error occurs
     * @see #eachLine(java.lang.String, int, groovy.lang.Function2)
     */
    static Object eachLine(String self, Function2<String, Integer, ?> closure) {
        eachLine(self, 0, closure)
    }

    /**
     * Iterates through this String line by line.  Each line is passed
     * to the given 1 arg closure.
     *
     * @param self a String
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure a closure (arg 1 is line)
     * @return the last value returned by the closure
     * @throws java.io.IOException if an error occurs
     */
    static Object eachLine(String self, int firstLine, Function1<String, ?> closure) {
        callFunctionForEachLine(self, firstLine, closure)
    }

    /**
     * Iterates through this String line by line.  Each line is passed
     * to the 2 arg closure. The line count is passed as the second argument.
     *
     * @param self a String
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure a closure (arg 1 is line, arg 2 is line number)
     * @return the last value returned by the closure
     * @throws java.io.IOException if an error occurs
     */
    static Object eachLine(String self, int firstLine, Function2<String, Integer, ?> closure) {
        callFunctionForEachLine(self, firstLine, closure)
    }

    /**
     * Iterates through this file line by line.  Each line is passed to the
     * given 1 arg closure.  The file is read using a reader which
     * is closed before this method returns.
     *
     * @param self a File
     * @param closure a closure (arg 1 is line)
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #eachLine(java.io.File, int, groovy.lang.Function1)
     */
    static Object eachLine(File self, Function1<String, ?> closure) throws IOException {
        eachLine(self, 1, closure)
    }

    /**
     * Iterates through this file line by line.  Each line is passed to the
     * given 2 arg closure.  The file is read using a reader which
     * is closed before this method returns.
     *
     * @param self a File
     * @param charset opens the file with a specified charset
     * @param closure a closure (arg 1 is line, arg 2 is line number starting at line 1)
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #eachLine(java.io.File, java.lang.String, int, groovy.lang.Function2)
     */
    static Object eachLine(File self, Function2<String, Integer, ?> closure) throws IOException {
        eachLine(self, 1, closure)
    }

    /**
     * Iterates through this file line by line.  Each line is passed
     * to the given 1 arg closure.  The file is read using a reader
     * which is closed before this method returns.
     *
     * @param self a File
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure a closure (arg 1 is line)
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #eachLine(java.io.Reader, int, groovy.lang.Function1)
     */
    static Object eachLine(File self, int firstLine, Function1<String, ?> closure) throws IOException {
        eachLine(DefaultGroovyMethods.newReader(self), firstLine, closure)
    }

    /**
     * Iterates through this file line by line.  Each line is passed
     * to the given 2 arg closure.  The file is read using a reader
     * which is closed before this method returns.
     *
     * @param self a File
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure a closure (arg 1 is line arg 2 is line number)
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #eachLine(java.io.Reader, int, groovy.lang.Function2)
     */
    static Object eachLine(File self, int firstLine, Function2<String, Integer, ?> closure) throws IOException {
        eachLine(DefaultGroovyMethods.newReader(self), firstLine, closure)
    }

    /**
     * Iterates through this file line by line.  Each line is passed to the
     * given  2 arg closure.  The file is read using a reader which
     * is closed before this method returns.
     *
     * @param self a File
     * @param charset opens the file with a specified charset
     * @param closure a closure (arg 1 is line)
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #eachLine(java.io.File, java.lang.String, int, groovy.lang.Function1)
     * @since 1.6.8
     */
    static Object eachLine(File self, String charset, Function1<String, ?> closure) throws IOException {
        eachLine(self, charset, 1, closure)
    }

    /**
     * Iterates through this file line by line.  Each line is passed to the
     * given 2 arg closure.  The file is read using a reader which
     * is closed before this method returns.
     *
     * @param self a File
     * @param charset opens the file with a specified charset
     * @param closure a closure (arg 1 is line, arg 2 is line number starting at line 1)
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #eachLine(java.io.File, java.lang.String, int, groovy.lang.Function2)
     * @since 1.6.8
     */
    static Object eachLine(File self, String charset, Function2<String, Integer, ?> closure) throws IOException {
        eachLine(self, charset, 1, closure)
    }

    /**
     * Iterates through this file line by line.  Each line is passed
     * to the given 1 arg closure.  The file is read using a reader
     * which is closed before this method returns.
     *
     * @param self a File
     * @param charset opens the file with a specified charset
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure a closure (arg 1 is line)
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #eachLine(java.io.Reader, int, groovy.lang.Function1)
     */
    static Object eachLine(File self, String charset, int firstLine, Function1<String, ?> closure) throws IOException {
        eachLine(DefaultGroovyMethods.newReader(self, charset), firstLine, closure)
    }

    /**
     * Iterates through this file line by line.  Each line is passed
     * to the given 2 arg closure.  The file is read using a reader
     * which is closed before this method returns.
     *
     * @param self a File
     * @param charset opens the file with a specified charset
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure a closure (arg 1 is line, arg 2 is line number)
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #eachLine(java.io.Reader, int, groovy.lang.Function2)
     */
    static Object eachLine(File self, String charset, int firstLine, Function2<String, Integer, ?> closure) throws IOException {
        eachLine(DefaultGroovyMethods.newReader(self, charset), firstLine, closure)
    }

    /**
     * Iterates through this stream reading with the provided charset, passing each line to the
     * given 1 arg closure.  The stream is closed before this method returns.
     *
     * @param stream a stream
     * @param charset opens the stream with a specified charset
     * @param closure a closure (arg 1 is line)
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #eachLine(java.io.InputStream, java.lang.String, int, groovy.lang.Function1)
     */
    static Object eachLine(InputStream stream, String charset, Function1<String, ?> closure) throws IOException {
        eachLine(stream, charset, 1, closure)
    }

    /**
     * Iterates through this stream reading with the provided charset, passing each line to
     * the given 1 arg closure.  The stream is closed after this method returns.
     *
     * @param stream a stream
     * @param charset opens the stream with a specified charset
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure a closure (arg 1 is line)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #eachLine(java.io.Reader, int, groovy.lang.Function1)
     * @since 1.5.7
     */
    static Object eachLine(InputStream stream, String charset, int firstLine, Function1<String, ?> closure)
    throws IOException {
        eachLine(new InputStreamReader(stream, charset), firstLine, closure)
    }

    /**
     * Iterates through this stream reading with the provided charset, passing each line to
     * the given 2 arg closure.  The stream is closed after this method returns.
     *
     * @param stream a stream
     * @param charset opens the stream with a specified charset
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure a closure (arg 1 is line, arg 2 is line number)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #eachLine(java.io.Reader, int, groovy.lang.Function2)
     * @since 1.5.7
     */
    static Object eachLine(InputStream stream, String charset, int firstLine, Function2<String, Integer, ?> closure)
    throws IOException {
        eachLine(new InputStreamReader(stream, charset), firstLine, closure)
    }

    /**
     * Iterates through this stream reading with the provided charset, passing each line to the
     * given 2 arg closure.  The stream is closed before this method returns.
     *
     * @param stream a stream
     * @param charset opens the stream with a specified charset
     * @param closure a closure (arg 1 is line, arg 2 is line number starting at line 1)
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #eachLine(java.io.InputStream, java.lang.String, int, groovy.lang.Function2)
     */
    static Object eachLine(InputStream stream, String charset, Function2<String, Integer, ?> closure)
    throws IOException {
        eachLine(new InputStreamReader(stream, charset), 1, closure)
    }

    /**
     * Iterates through this stream, passing each line to the given 1 arg closure.
     * The stream is closed before this method returns.
     *
     * @param stream a stream
     * @param closure a closure (arg 1 is line)
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #eachLine(java.io.InputStream, int, groovy.lang.Function1)
     */
    static Object eachLine(InputStream stream, Function1<String, ?> closure) throws IOException {
        eachLine(stream, 1, closure)
    }

    /**
     * Iterates through this stream, passing each line to the given 2 arg closure.
     * The stream is closed before this method returns.
     *
     * @param stream a stream
     * @param closure a closure (arg 1 is line arg 2 is line number starting at line 1)
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #eachLine(java.io.InputStream, int, groovy.lang.Function2)
     */
    static Object eachLine(InputStream stream, Function2<String, Integer, ?> closure)
    throws IOException {
        eachLine(stream, 1, closure)
    }

    /**
     * Iterates through this stream, passing each line to the given 1 arg closure.
     * The stream is closed before this method returns.
     *
     * @param stream a stream
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure a closure (arg 1 is line)
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #eachLine(java.io.Reader, int, groovy.lang.Function1)
     */
    static Object eachLine(InputStream stream, int firstLine, Function1<String, ?> closure) throws IOException {
        eachLine(new InputStreamReader(stream), firstLine, closure)
    }

    /**
     * Iterates through this stream, passing each line to the given 2 arg closure.
     * The stream is closed before this method returns.
     *
     * @param stream a stream
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure a closure (arg 1 is line, arg 2 is line number)
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #eachLine(java.io.Reader, int, groovy.lang.Function2)
     */
    static Object eachLine(InputStream stream, int firstLine, Function2<String, Integer, ?> closure)
    throws IOException {
        eachLine(new InputStreamReader(stream), firstLine, closure)
    }

    // Reader
    /**
     * Iterates through the given reader line by line.  Each line is passed to the
     * given 1 arg closure. The Reader is closed before this method returns.
     *
     * @param self a Reader, closed after the method returns
     * @param closure a closure (arg 1 is line)
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #eachLine(java.io.Reader, int, groovy.lang.Function1)
     */
    static Object eachLine(Reader self, Function1<String, ?> closure) throws IOException {
        eachLine(self, 1, closure)
    }

    /**
     * Iterates through the given reader line by line.  Each line is passed to the
     * given 1 2 arg closure. The line count is passed
     * as the second argument. The Reader is closed before this method returns.
     *
     * @param self a Reader, closed after the method returns
     * @param closure a closure (arg 1 is line, arg 2 is line number starting at line 1)
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #eachLine(java.io.Reader, int, groovy.lang.Function1)
     */
    static Object eachLine(Reader self, Function2<String, Integer, ?> closure) throws IOException {
        eachLine(self, 1, closure)
    }

    /**
     * Iterates through the given reader line by line.  Each line is passed to the
     * given 1 or 2 arg closure. If the closure has two arguments, the line count is passed
     * as the second argument. The Reader is closed before this method returns.
     *
     * @param self a Reader, closed after the method returns
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure a closure which will be passed each line
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    static Object eachLine(Reader self, int startIndex, Function1<String, ?> closure) throws IOException {
        callFunctionForEachLine(self, startIndex, closure)
    }

    /**
     * Iterates through the given reader line by line.  Each line is passed to the
     * given 1 or 2 arg closure. If the closure has two arguments, the line count is passed
     * as the second argument. The Reader is closed before this method returns.
     *
     * @param self a Reader, closed after the method returns
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure a closure which will be passed the line and line count
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    static Object eachLine(Reader self, int startIndex, Function2<String, Integer, ?> closure) throws IOException {
        callFunctionForEachLine(self, startIndex, closure)
    }

    // URLs
    /**
     * Iterates through the lines read from the URL's associated input stream passing each
     * line to the given 1 arg closure. The stream is closed before this method returns.
     *
     * @param url a URL to open and read
     * @param closure a closure to apply on each line (arg 1 is line)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #eachLine(java.net.URL, int, groovy.lang.Function1)
     */
    static Object eachLine(URL url, Function1<String, ?> closure) throws IOException {
        eachLine(url, 1, closure)
    }

    /**
     * Iterates through the lines read from the URL's associated input stream passing each
     * line to the given 2 arg closure. The stream is closed before this method returns.
     *
     * @param url a URL to open and read
     * @param closure a closure to apply on each line (arg 1 is line, arg 2 is line number starting at line 1)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #eachLine(java.net.URL, int, groovy.lang.Function2)
     */
    static Object eachLine(URL url, Function2<String, Integer, ?> closure) throws IOException {
        eachLine(url, 1, closure)
    }

    /**
     * Iterates through the lines read from the URL's associated input stream passing each
     * line to the given 1 arg closure. The stream is closed before this method returns.
     *
     * @param url a URL to open and read
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure a closure to apply on each line (arg 1 is line)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #eachLine(java.io.InputStream, int, groovy.lang.Function1)
     */
    static Object eachLine(URL url, int startIndex, Function1<String, ?> closure) throws IOException {
        eachLine(url.openConnection().getInputStream(), startIndex, closure)
    }

    /**
     * Iterates through the lines read from the URL's associated input stream passing each
     * line to the given 2 arg closure. The stream is closed before this method returns.
     *
     * @param url a URL to open and read
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure a closure to apply on each line (arg 1 is line, arg 2 is line number)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #eachLine(java.io.InputStream, int, groovy.lang.Function2)
     * @since 1.5.7
     */
    static Object eachLine(URL url, int startIndex, Function2<String, Integer, ?> closure) throws IOException {
        eachLine(url.openConnection().getInputStream(), startIndex, closure)
    }

    /**
     * Iterates through the lines read from the URL's associated input stream passing each
     * line to the given 1 arg closure. The stream is closed before this method returns.
     *
     * @param url a URL to open and read
     * @param charset opens the stream with a specified charset
     * @param closure a closure to apply on each line (arg 1 is line)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #eachLine(java.net.URL, java.lang.String, int, groovy.lang.Function1)
     */
    static Object eachLine(URL url, String charset, Function1<String, ?> closure) throws IOException {
        eachLine(url, charset, 1, closure)
    }

    /**
     * Iterates through the lines read from the URL's associated input stream passing each
     * line to the given 2 arg closure. The stream is closed before this method returns.
     *
     * @param url a URL to open and read
     * @param charset opens the stream with a specified charset
     * @param closure a closure to apply on each line (arg 1 is line, arg 2 is line number starting at line 1)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #eachLine(java.net.URL, java.lang.String, int, groovy.lang.Function2)
     * @since 1.5.6
     */
    static Object eachLine(URL url, String charset, Function2<String, Integer, ?> closure) throws IOException {
        eachLine(url, charset, 1, closure)
    }

    /**
     * Iterates through the lines read from the URL's associated input stream passing each
     * line to the given 1 arg closure. The stream is closed before this method returns.
     *
     * @param url a URL to open and read
     * @param charset opens the stream with a specified charset
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure a closure to apply on each line (arg 1 is line)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #eachLine(java.io.Reader, int, groovy.lang.Function1)
     */
    static Object eachLine(URL url, String charset, int startIndex, Function1<String, ?> closure)
    throws IOException {
        eachLine(DefaultGroovyMethods.newReader(url, charset), startIndex, closure)
    }

    /**
     * Iterates through the lines read from the URL's associated input stream passing each
     * line to the given 2 arg closure. The stream is closed before this method returns.
     *
     * @param url a URL to open and read
     * @param charset opens the stream with a specified charset
     * @param firstLine the line number value used for the first line (default is 1, set to 0 to start counting from 0)
     * @param closure a closure to apply on each line (arg 1 is line, optional arg 2 is line number)
     * @return the last value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #eachLine(java.io.Reader, int, groovy.lang.Function2)
     */
    static Object eachLine(URL url, String charset, int startIndex, Function2<String, Integer, ?> closure)
    throws IOException {
        eachLine(DefaultGroovyMethods.newReader(url, charset), startIndex, closure)
    }

    private static Object callFunctionForEachLine(String self, int firstLine, def closure) {
        int count = firstLine
        String line = null
        for (String l: self.readLines()) {
            line = l
            callFunctionForLine(line, count, closure)
            count++
        }
        return line
    }

    private static Object callFunctionForEachLine(Reader self, int startIndex, def closure) {
        BufferedReader br
        int count = startIndex
        String result = null

        if (self instanceof BufferedReader)
            br = (BufferedReader) self
        else
            br = new BufferedReader(self)

        try {
            while (true) {
                String line = br.readLine()
                if (line == null) {
                    break
                } else {
                    result = callFunctionForLine(line, count, closure)
                    count++
                }
            }
            Reader temp = self
            self = null
            temp.close()
            return result
        } finally {
            DefaultGroovyMethodsSupport.closeWithWarning(self)
            DefaultGroovyMethodsSupport.closeWithWarning(br)
        }
    }

    private static Object callFunctionForLine(String line, int index, def closure) {
        if (closure instanceof Function1)
            ((Function1) closure)(line)
        else if (closure instanceof Function2)
            ((Function2) closure)(line, index)
        else throw new IllegalArgumentException("Function1 or Function2 expected")

        line
    }

    // Splits
    // File
    /**
     * Iterates through this file line by line, splitting each line using
     * the given regex separator. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression.
     * Finally the resources used for processing the file are closed.
     *
     * @param self a File
     * @param regex the delimiting regular expression
     * @param closure a closure
     * @throws IOException if an IOException occurs.
     * @throws java.util.regex.PatternSyntaxException if the regular expression's syntax is invalid
     * @return the last value returned by the closure
     * @see #splitEachLine(java.io.Reader, java.lang.String, groovy.lang.Function1)
     */
    public static Object splitEachLine(File self, String regex, Function1<List<String>, ?> closure) throws IOException {
        return splitEachLine(DefaultGroovyMethods.newReader(self), regex, closure)
    }

    /**
     * Iterates through this file line by line, splitting each line using
     * the given separator Pattern. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression Pattern.
     * Finally the resources used for processing the file are closed.
     *
     * @param self a File
     * @param pattern the regular expression Pattern for the delimiter
     * @param closure a closure
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #splitEachLine(java.io.Reader, java.util.regex.Pattern, groovy.lang.Function1)
     */
    public static Object splitEachLine(File self, Pattern pattern, Function1<List<String>, ?> closure) throws IOException {
        return splitEachLine(DefaultGroovyMethods.newReader(self), pattern, closure)
    }

    /**
     * Iterates through this file line by line, splitting each line using
     * the given regex separator. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression.
     * Finally the resources used for processing the file are closed.
     *
     * @param self a File
     * @param regex the delimiting regular expression
     * @param charset opens the file with a specified charset
     * @param closure a closure
     * @throws IOException if an IOException occurs.
     * @throws java.util.regex.PatternSyntaxException if the regular expression's syntax is invalid
     * @return the last value returned by the closure
     * @see #splitEachLine(java.io.Reader, java.lang.String, groovy.lang.Function1)
     */
    public static Object splitEachLine(File self, String regex, String charset, Function1<List<String>, ?> closure) throws IOException {
        return splitEachLine(DefaultGroovyMethods.newReader(self, charset), regex, closure)
    }

    /**
     * Iterates through this file line by line, splitting each line using
     * the given regex separator Pattern. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression.
     * Finally the resources used for processing the file are closed.
     *
     * @param self a File
     * @param pattern the regular expression Pattern for the delimiter
     * @param charset opens the file with a specified charset
     * @param closure a closure
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #splitEachLine(java.io.Reader, java.util.regex.Pattern, groovy.lang.Function1)
     */
    public static Object splitEachLine(File self, Pattern pattern, String charset, Function1<List<String>, ?> closure) throws IOException {
        return splitEachLine(DefaultGroovyMethods.newReader(self, charset), pattern, closure)
    }

    /**
     * Iterates through the input stream associated with this URL line by line, splitting each line using
     * the given regex separator. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression.
     * Finally the resources used for processing the URL are closed.
     *
     * @param self a URL to open and read
     * @param regex the delimiting regular expression
     * @param closure a closure
     * @throws IOException if an IOException occurs.
     * @throws java.util.regex.PatternSyntaxException if the regular expression's syntax is invalid
     * @return the last value returned by the closure
     * @see #splitEachLine(java.io.Reader, java.lang.String, groovy.lang.Function1)
     */
    public static Object splitEachLine(URL self, String regex, Function1<List<String>, ?> closure) throws IOException {
        return splitEachLine(DefaultGroovyMethods.newReader(self), regex, closure)
    }

    /**
     * Iterates through the input stream associated with this URL line by line, splitting each line using
     * the given regex separator Pattern. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression.
     * Finally the resources used for processing the URL are closed.
     *
     * @param self a URL to open and read
     * @param pattern the regular expression Pattern for the delimiter
     * @param closure a closure
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #splitEachLine(java.io.Reader, java.util.regex.Pattern, groovy.lang.Function1)
     */
    public static Object splitEachLine(URL self, Pattern pattern, Function1<List<String>, ?> closure) throws IOException {
        return splitEachLine(DefaultGroovyMethods.newReader(self), pattern, closure)
    }

    /**
     * Iterates through the input stream associated with this URL line by line, splitting each line using
     * the given regex separator. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression.
     * Finally the resources used for processing the URL are closed.
     *
     * @param self a URL to open and read
     * @param regex the delimiting regular expression
     * @param charset opens the file with a specified charset
     * @param closure a closure
     * @throws IOException if an IOException occurs.
     * @throws java.util.regex.PatternSyntaxException if the regular expression's syntax is invalid
     * @return the last value returned by the closure
     * @see #splitEachLine(java.io.Reader, java.lang.String, groovy.lang.Function1)
     */
    public static Object splitEachLine(URL self, String regex, String charset, Function1<List<String>, ?> closure) throws IOException {
        return splitEachLine(DefaultGroovyMethods.newReader(self, charset), regex, closure)
    }

    /**
     * Iterates through the input stream associated with this URL line by line, splitting each line using
     * the given regex separator Pattern. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression.
     * Finally the resources used for processing the URL are closed.
     *
     * @param self a URL to open and read
     * @param pattern the regular expression Pattern for the delimiter
     * @param charset opens the file with a specified charset
     * @param closure a closure
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #splitEachLine(java.io.Reader, java.util.regex.Pattern, groovy.lang.Function1)
     */
    public static Object splitEachLine(URL self, Pattern pattern, String charset, Function1<List<String>, ?> closure) throws IOException {
        return splitEachLine(DefaultGroovyMethods.newReader(self, charset), pattern, closure)
    }

    // InputStream
    /**
     * Iterates through the given InputStream line by line using the specified
     * encoding, splitting each line using the given separator.  The list of tokens
     * for each line is then passed to the given closure. Finally, the stream
     * is closed.
     *
     * @param stream an InputStream
     * @param regex the delimiting regular expression
     * @param charset opens the stream with a specified charset
     * @param closure a closure
     * @throws IOException if an IOException occurs.
     * @throws java.util.regex.PatternSyntaxException if the regular expression's syntax is invalid
     * @return the last value returned by the closure
     * @see #splitEachLine(java.io.Reader, java.lang.String, groovy.lang.Function1)
     */
    public static Object splitEachLine(InputStream self, String regex, String charset, Function1<List<String>, ?> closure) throws IOException {
        return splitEachLine(DefaultGroovyMethods.newReader(self, charset), regex, closure)
    }

    /**
     * Iterates through the given InputStream line by line using the specified
     * encoding, splitting each line using the given separator Pattern.  The list of tokens
     * for each line is then passed to the given closure. Finally, the stream
     * is closed.
     *
     * @param stream an InputStream
     * @param pattern the regular expression Pattern for the delimiter
     * @param charset opens the stream with a specified charset
     * @param closure a closure
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #splitEachLine(java.io.Reader, java.util.regex.Pattern, groovy.lang.Function1)
     */
    public static Object splitEachLine(InputStream self, Pattern pattern, String charset, Function1<List<String>, ?> closure) throws IOException {
        return splitEachLine(DefaultGroovyMethods.newReader(self, charset), pattern, closure)
    }

    /**
     * Iterates through the given InputStream line by line, splitting each line using
     * the given separator.  The list of tokens for each line is then passed to
     * the given closure. The stream is closed before the method returns.
     *
     * @param stream an InputStream
     * @param regex the delimiting regular expression
     * @param closure a closure
     * @throws IOException if an IOException occurs.
     * @throws java.util.regex.PatternSyntaxException if the regular expression's syntax is invalid
     * @return the last value returned by the closure
     * @see #splitEachLine(java.io.Reader, java.lang.String, groovy.lang.Function1)
     */
    public static Object splitEachLine(InputStream self, String regex, Function1<List<String>, ?> closure) throws IOException {
        return splitEachLine(DefaultGroovyMethods.newReader(self), regex, closure)
    }

    /**
     * Iterates through the given InputStream line by line, splitting each line using
     * the given separator Pattern.  The list of tokens for each line is then passed to
     * the given closure. The stream is closed before the method returns.
     *
     * @param stream an InputStream
     * @param pattern the regular expression Pattern for the delimiter
     * @param closure a closure
     * @throws IOException if an IOException occurs.
     * @return the last value returned by the closure
     * @see #splitEachLine(java.io.Reader, java.util.regex.Pattern, groovy.lang.Function1)
     */
    public static Object splitEachLine(InputStream self, Pattern pattern, Function1<List<String>, ?> closure) throws IOException {
        return splitEachLine(DefaultGroovyMethods.newReader(self), pattern, closure)
    }

    // Reader
    /**
     * Iterates through the given reader line by line, splitting each line using
     * the given regex separator. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression.  The Reader is closed afterwards.
     * <p/>
     * Here is an example:
     * <pre>
     * def s = 'The 3 quick\nbrown 4 fox'
     * def result = ''
     * new StringReader(s).splitEachLine(/\d/){ parts ->
     *     result += "${parts[0]}_${parts[1]}|"
     *}* assert result == 'The _ quick|brown _ fox|'
     * </pre>
     *
     * @param self a Reader, closed after the method returns
     * @param regex the delimiting regular expression
     * @param closure a closure
     * @throws IOException if an IOException occurs.
     * @throws java.util.regex.PatternSyntaxException if the regular expression's syntax is invalid
     * @return the last value returned by the closure
     * @see java.lang.String#split(java.lang.String)
     */
    public static Object splitEachLine(Reader self, String regex, Function1<List<String>, ?> closure) throws IOException {
        return splitEachLine(self, Pattern.compile(regex), closure)
    }

    /**
     * Iterates through the given reader line by line, splitting each line using
     * the given regex separator Pattern. For each line, the given closure is called with
     * a single parameter being the list of strings computed by splitting the line
     * around matches of the given regular expression.  The Reader is closed afterwards.
     * <p/>
     * Here is an example:
     * <pre>
     * def s = 'The 3 quick\nbrown 4 fox'
     * def result = ''
     * new StringReader(s).splitEachLine(~/\d/){ parts ->
     *     result += "${parts[0]}_${parts[1]}|"
     *}* assert result == 'The _ quick|brown _ fox|'
     * </pre>
     *
     * @param self a Reader, closed after the method returns
     * @param pattern the regular expression Pattern for the delimiter
     * @param closure a closure
     * @throws IOException if an IOException occurs.
     * @throws java.util.regex.PatternSyntaxException if the regular expression's syntax is invalid
     * @return the last value returned by the closure
     * @see java.lang.String#split(java.lang.String)
     */
    public static Object splitEachLine(Reader self, Pattern pattern, Function1<List<String>, ?> closure) throws IOException {
        BufferedReader br
        Object result = null

        if (self instanceof BufferedReader)
            br = (BufferedReader) self
        else
            br = new BufferedReader(self)

        try {
            while (true) {
                String line = br.readLine()
                if (line == null) {
                    break
                } else {
                    List vals = Arrays.asList(pattern.split(line))
                    result = closure(vals)
                }
            }
            Reader temp = self
            self = null
            temp.close()
            return result
        } finally {
            DefaultGroovyMethodsSupport.closeWithWarning(self)
            DefaultGroovyMethodsSupport.closeWithWarning(br)
        }
    }

    // String

    /**
     * Iterates through the given String line by line, splitting each line using
     * the given separator.  The list of tokens for each line is then passed to
     * the given closure.
     *
     * @param self a String
     * @param regex the delimiting regular expression
     * @param closure a closure
     * @return the last value returned by the closure
     * @throws java.io.IOException if an error occurs
     * @throws java.util.regex.PatternSyntaxException if the regular expression's syntax is invalid
     * @see java.lang.String#split(java.lang.String)
     */
    public static Object splitEachLine(String self, String regex, Function1<List<String>, ?> closure) throws IOException {
        return splitEachLine(self, Pattern.compile(regex), closure)
    }

    /**
     * Iterates through the given String line by line, splitting each line using
     * the given separator Pattern.  The list of tokens for each line is then passed to
     * the given closure.
     *
     * @param self a String
     * @param pattern the regular expression Pattern for the delimiter
     * @param closure a closure
     * @return the last value returned by the closure
     * @throws java.io.IOException if an error occurs
     * @see java.util.regex.Pattern#split(java.lang.String)
     */
    public static Object splitEachLine(String self, Pattern pattern, Function1<List<String>, ?> closure) throws IOException {
        final List<String> list = self.readLines()
        Object result = null
        for (String line: list) {
            List vals = Arrays.asList(pattern.split(line))
            result = closure(vals)
        }
        return result
    }
}