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

  void testEqualsTilde() {
    shell.evaluate """
      @Typed
      def u () {
        assert "cheesecheese" =~ "cheese"
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


}