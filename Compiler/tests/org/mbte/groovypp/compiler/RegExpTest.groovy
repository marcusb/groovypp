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

package org.mbte.groovypp.compiler

public class RegExpTest extends GroovyShellTestCase {
  void testSimpleRegExps() {
    shell.evaluate """
      import java.util.regex.Matcher
      import java.util.regex.Pattern

      @Typed
      def u () {
        def pattern = ~/foo/
        assert pattern instanceof Pattern
        assert pattern.matcher("foo").matches()
        assert !pattern.matcher("foobar").matches()

        assert "cheese" == /cheese/
      }

      u()
    """
  }

  void testNumericMatch() {
    shell.evaluate """
      @Typed
      def u () {
        def pattern = ~/\\d+/
        assert pattern.matcher("2009").matches()
        assert !pattern.matcher("groovy").matches()
      }
      u()
    """
  }

  void testReplace() {
    shell.evaluate """
      @Typed
      def u () {
        def cheese = (~/cheese/).matcher("cheesecheese").replaceFirst("nice")
        assert cheese == "nicecheese"

        assert "color" == "colour".replaceFirst(/ou/, "o")

        cheese = (~/cheese/).matcher("cheesecheese").replaceAll("nice")
        assert cheese == "nicenice"
       }
      u()
    """
  }

  void testGroup() {
    shell.evaluate """
      @Typed
      def u () {
        def groupMatcher = (~/o(b.*r)f/).matcher("foobarfoo")
        assert groupMatcher[0] == ["obarf", "bar"]
       }
      u()
    """
  }

  void testMatcherRanges() {
    shell.evaluate """
      @Typed
      def u () {
        def matcher = (~/([^e]+)e+/).matcher("cheese please")
        assert ["se", "s"] == matcher[1]
        assert [["se", "s"], [" ple", " pl"]] == matcher[1, 2]
        assert [["se", "s"], [" ple", " pl"]] == matcher[1 .. 2]
        assert [["chee", "ch"], [" ple", " pl"], ["ase", "as"]] == matcher[0, 2..3]
       }
      u()
    """
  }

  void testMatcherIterator() {
    shell.evaluate """
      @Typed
      def u () {
        def matcher = (~/([^e]+)e+/).matcher("cheese please")
        assert matcher.collect { it }  == [["chee", "ch"], ["se", "s"], [" ple", " pl"], ["ase", "as"]]
       }
      u()
    """
  }

  void testGrepMatcher() {
    shell.evaluate """
      @Typed
      def u () {
        assert ["foo", "moo"] == ["foo", "bar", "moo"].grep(~/.*oo\$/)
      }
      u()
    """
  }

  void testEqualsTilde() {
    shell.evaluate """
      @Typed
      def u () {
        def a = ("cheesecheese" =~ "cheese");
        assert a instanceof java.util.regex.Matcher
        assert("cheesecheese" =~ "cheese")
        assert !("cheese" =~ "cheesecheese")
        assert !("cheesecheese" =~ "asdf")
      }
      u()
    """
  }

  void testCompareTilde() {
    shell.evaluate """
      @Typed
      def u () {
        assert "2009" ==~ /\\d+/
      }
      u()
    """
  }

  void testBitwiseNegateGStringSyntax() {
    shell.evaluate """
      @Typed
      def u () {
          def value = "foo"
          def pattern = ~"\$value"
          assert pattern instanceof java.util.regex.Pattern
      }
      u()
    """
  }
}