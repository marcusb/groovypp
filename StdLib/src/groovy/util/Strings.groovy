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
	static Object eachLine(String self, Function1<String,?> closure) {
		eachLine (self, 0, closure)
	}

	static Object eachLine(String self, Function2<String, Integer, ?> closure) {
		eachLine (self, 0, closure)
	}

	static Object eachLine(String self, int firstLine, Function1<String,?> closure) {
		callFunctionForEachLine (self, firstLine, closure)
	}

	static Object eachLine(String self, int firstLine, Function2<String,Integer,?> closure) {
		callFunctionForEachLine (self, firstLine, closure)
	}

	// File
	static Object eachLine(File self, Function1<String, ?> closure) throws IOException {
	    eachLine(self, 1, closure)
	}

	static Object eachLine(File self, Function2<String, Integer, ?> closure) throws IOException {
	    eachLine(self, 1, closure)
	}

	static Object eachLine(File self, int firstLine, Function1<String, ?> closure) throws IOException {
		eachLine(DefaultGroovyMethods.newReader(self), firstLine, closure)
	}

	static Object eachLine(File self, int firstLine, Function2<String, Integer, ?> closure) throws IOException {
	    eachLine(DefaultGroovyMethods.newReader(self), firstLine, closure)
	}

	static Object eachLine(File self, String charset, Function1<String, ?> closure) throws IOException {
		eachLine(self, charset, 1, closure)
	}

	static Object eachLine(File self, String charset, Function2<String, Integer, ?> closure) throws IOException {
	    eachLine(self, charset, 1, closure)
	}

	static Object eachLine(File self, String charset, int firstLine, Function1<String, ?> closure) throws IOException {
		eachLine(DefaultGroovyMethods.newReader(self, charset), firstLine, closure)
	}

	static Object eachLine(File self, String charset,  int firstLine, Function2<String, Integer, ?> closure) throws IOException {
	    eachLine(DefaultGroovyMethods.newReader(self, charset), firstLine, closure)
	}

	// STREAMS
	static Object eachLine(InputStream stream, String charset, Function1<String, ?> closure) throws IOException {
	    eachLine(stream, charset, 1, closure)
	}

	static Object eachLine(InputStream stream, String charset, int firstLine, Function1<String, ?> closure)
			throws IOException {
		eachLine(new InputStreamReader(stream, charset), firstLine, closure)
	}

	static Object eachLine(InputStream stream, String charset, Function2<String, Integer, ?> closure)
			throws IOException {
	    eachLine(new InputStreamReader(stream, charset), 1, closure)
	}

	static Object eachLine(InputStream stream, String charset, int firstLine, Function2<String, Integer, ?> closure)
			throws IOException {
		eachLine(new InputStreamReader(stream, charset), firstLine, closure)
	}

	static Object eachLine(InputStream stream, Function1<String, ?> closure) throws IOException {
		eachLine(stream, 1, closure)
	}

	static Object eachLine(InputStream stream, Function2<String, Integer, ?> closure)
			throws IOException {
		eachLine(stream, 1, closure)
	}

	static Object eachLine(InputStream stream, int firstLine, Function1<String, ?> closure) throws IOException {
		eachLine(new InputStreamReader(stream), firstLine, closure)
	}

	static Object eachLine(InputStream stream, int firstLine, Function2<String, Integer, ?> closure)
			throws IOException {
		eachLine(new InputStreamReader(stream), firstLine, closure)
	}

	// Reader
	static Object eachLine(Reader self, Function1<String,?> closure) throws IOException {
	    eachLine(self, 1, closure)
	}

	static Object eachLine(Reader self, Function2<String, Integer, ?> closure) throws IOException {
	    eachLine(self, 1, closure)
	}

	static Object eachLine(Reader self, int startIndex, Function1<String,?> closure) throws IOException {
		callFunctionForEachLine(self, startIndex, closure)
	}

	static Object eachLine(Reader self, int startIndex, Function2<String, Integer, ?> closure) throws IOException {
		callFunctionForEachLine(self, startIndex, closure)
	}

	// URLs
	static Object eachLine(URL url, Function1<String, ?> closure) throws IOException {
	    eachLine(url, 1, closure)
	}

	static Object eachLine(URL url, Function2<String, Integer, ?> closure) throws IOException {
	    eachLine(url, 1, closure)
	}

	static Object eachLine(URL url, int startIndex, Function1<String, ?> closure) throws IOException {
		eachLine(url.openConnection().getInputStream(), startIndex, closure)
	}

	static Object eachLine(URL url, int startIndex, Function2<String, Integer, ?> closure) throws IOException {
		eachLine(url.openConnection().getInputStream(), startIndex, closure)
	}

	static Object eachLine(URL url, String charset, Function1<String, ?> closure) throws IOException {
	    eachLine(url, charset, 1, closure)
	}

	static Object eachLine(URL url, String charset, Function2<String, Integer, ?> closure) throws IOException {
	    eachLine(url, charset, 1, closure)
	}

	static Object eachLine(URL url, String charset, int startIndex, Function1<String, ?> closure)
			throws IOException {
		eachLine(DefaultGroovyMethods.newReader(url, charset), startIndex, closure)
	}

	static Object eachLine(URL url, String charset, int startIndex, Function2<String, Integer, ?> closure)
			throws IOException {
		eachLine(DefaultGroovyMethods.newReader(url, charset), startIndex, closure)
	}

	private static Object callFunctionForEachLine(String self, int firstLine, def closure) {
		int count = firstLine
		String line = null
		for (String l : self.readLines()) {
			line = l
			callFunctionForLine (line, count, closure)
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
	        ((Function1)closure)(line)
		else if (closure instanceof Function2)
			((Function2)closure)(line, index)
		else throw new IllegalArgumentException("Function1 or Function2 expected")

		line
	}

	// Splits
	// File
	public static Object splitEachLine(File self, String regex, Function1<List<String>, ?> closure) throws IOException {
	    return splitEachLine(DefaultGroovyMethods.newReader(self), regex, closure)
	}

	public static Object splitEachLine(File self, Pattern pattern, Function1<List<String>, ?> closure) throws IOException {
	    return splitEachLine(DefaultGroovyMethods.newReader(self), pattern, closure)
	}

	public static Object splitEachLine(File self, String regex, String charset, Function1<List<String>, ?> closure) throws IOException {
	    return splitEachLine(DefaultGroovyMethods.newReader(self, charset), regex, closure)
	}

	public static Object splitEachLine(File self, Pattern pattern, String charset, Function1<List<String>, ?> closure) throws IOException {
	    return splitEachLine(DefaultGroovyMethods.newReader(self, charset), pattern, closure)
	}

	// URL
	public static Object splitEachLine(URL self, String regex, Function1<List<String>, ?> closure) throws IOException {
	    return splitEachLine(DefaultGroovyMethods.newReader(self), regex, closure)
	}

	public static Object splitEachLine(URL self, Pattern pattern, Function1<List<String>, ?> closure) throws IOException {
	    return splitEachLine(DefaultGroovyMethods.newReader(self), pattern, closure)
	}

	public static Object splitEachLine(URL self, String regex, String charset, Function1<List<String>, ?> closure) throws IOException {
	    return splitEachLine(DefaultGroovyMethods.newReader(self, charset), regex, closure)
	}

	public static Object splitEachLine(URL self, Pattern pattern, String charset, Function1<List<String>, ?> closure) throws IOException {
	    return splitEachLine(DefaultGroovyMethods.newReader(self, charset), pattern, closure)
	}

	// InputStream
	public static Object splitEachLine(InputStream self, String regex, String charset, Function1<List<String>, ?> closure) throws IOException {
	    return splitEachLine(DefaultGroovyMethods.newReader(self, charset), regex, closure)
	}

	 public static Object splitEachLine(InputStream self, Pattern pattern, String charset, Function1<List<String>, ?> closure) throws IOException {
        return splitEachLine(DefaultGroovyMethods.newReader(self, charset), pattern, closure)
    }

	public static Object splitEachLine(InputStream self, String regex, Function1<List<String>, ?> closure) throws IOException {
	    return splitEachLine(DefaultGroovyMethods.newReader(self), regex, closure)
	}

	public static Object splitEachLine(InputStream self, Pattern pattern, Function1<List<String>, ?> closure) throws IOException {
	    return splitEachLine(DefaultGroovyMethods.newReader(self), pattern, closure)
	}

	// Reader
	public static Object splitEachLine(Reader self, String regex, Function1<List<String>, ?> closure) throws IOException {
	    return splitEachLine(self, Pattern.compile(regex), closure)
	}

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
	public static Object splitEachLine(String self, String regex, Function1<List<String>, ?> closure) throws IOException {
        return splitEachLine(self, Pattern.compile(regex), closure)
    }

	public static Object splitEachLine(String self, Pattern pattern, Function1<List<String>, ?> closure) throws IOException {
        final List<String> list = self.readLines()
        Object result = null
        for (String line : list) {
            List vals = Arrays.asList(pattern.split(line))
            result = closure(vals)
        }
        return result
    }
}