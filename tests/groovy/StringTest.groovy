/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovy

/**
 * Various tests for Strings.
 *
 * @author Michael Baehr
 */
class StringTest extends GroovyShellTestCase {

  void testString() {
    shell.evaluate """

          @Typed
          def u() {
            def s = "abcd"
            assert s.length() == 4
            assert 4 == s.length()

            // test polymorphic size() method like collections
            assert s.size() == 4

            s = s + "efg" + "hijk"

            assert s.size() == 11
            assert "abcdef".size() == 6
          }

          u()
        """
  }

  void testStringPlusNull() {
    shell.evaluate """
          @Typed
          def u() {
            def y = null
            def x = "hello " + y
            assert x == "hello null"

          }

          u()
        """
  }

  void testNextPrevious() {
    shell.evaluate """
          @Typed
          def u() {
            def x = 'a'
            def y = x.next()
            assert y == 'b'

            def z = 'z'.previous()
            assert z == 'y'

            z = 'z'
            def b = z.next()
            assert b != 'z'

            assert b > z
            assert z.charAt(0) == 'z'
            assert b.charAt(0) == '{'
          }

          u()
        """
  }

  void testApppendToString() {
    shell.evaluate """
        @Typed
        def u() {
          def name = "Gromit"
          def result = "hello " << name << "!"

          assert result.toString() == "hello Gromit!"
        }

        u()
      """
  }

  void testApppendToStringBuffer() {
    shell.evaluate """
        @Typed
        def u() {
          def buffer = new StringBuffer()

          def name = "Gromit"
          buffer << "hello " << name << "!"

          assert buffer.toString() == "hello Gromit!"
        }
        u()
      """
  }


  void testSimpleStringLiterals() {
    def script = """
        @Typed
        def u() {
          assert "\\n".length() == 1
          assert "\\"".length() == 1
          assert "\\'".length() == 1
          assert "\\\\".length() == 1
          assert "\\\${0}".length() == 4
          assert "\\\${0}".indexOf("{0}") >= 0

          assert "x\\
y".length() == 2

          assert "x\\
y".indexOf("xy") >= 0

          assert '\\n'.length() == 1
          assert '\\''.length() == 1
          assert '\\\\'.length() == 1
          assert "\\\${0}".length() == 4
          assert "\\\${0}".indexOf("{0}") >= 0

          assert 'x\\
y'.length() == 2

          assert 'x\\
y'.indexOf('xy') >= 0


        }
        u()
      """
    shell.evaluate script;
  }

  void testMinusRemovesFirstOccurenceOfString() {
    shell.evaluate """

          @Typed
          def u() {
            assert "abcdeabcd" - 'bc' == 'adeabcd'
          }

          u()
        """

  }

  void testMinusEscapesRegexChars() {
    shell.evaluate """
          @Typed
          def u() {
            assert "abcdeab.d.f" - '.d.' == 'abcdeabf'
          }

          u()
        """
  }

  void testMultilineStringLiterals() {
    def script = """
        @Typed
        def u() {
          assert \"\"\"\"x\"\"\" == '"x'
          assert \"\"\"\""x""\" == '""x'
          assert \"\"\"x
y\"\"\" == 'x\\ny'

          assert \"\"\"\\n
\\n\"\"\" == '\\n\\n\\n'

          assert ''''x''' == "'x"
          assert '''''x''' == "''x"

          assert '''x
y''' == 'x\\ny'

          assert '''\\n
\\n''' == '\\n\\n\\n'
        }

        u()
      """
    shell.evaluate script;
  }

  void testRegexpStringLiterals() {
    def script = """
        @Typed
        def u() {
          assert "foo" == /foo/
          assert '\\\\\$\$' == /\\\$\$/
          assert "/\\\\*" == /\\/\\*/
          
        }
        u()
      """;
    shell.evaluate script;
  }

  void testBoolCoerce() {
    shell.evaluate """

          @Typed
          def u() {
            // Explicit coercion
            assert !((Boolean) "")
            assert ((Boolean) "content")

            // Implicit coercion in statements
            String s = null
            if (s) {
                assert false
            }
            s = ''
            if (s) {
                 assert false
            }
            s = 'something'
            if (!s) {
                 assert false
            }
          }

          u()
        """

  }

  void testSplit() {
    shell.evaluate """

        @Typed
        def u() {
          def text = "hello there\\nhow are you"
          def splitted = text.split()
          assert splitted == ['hello', 'there', 'how', 'are', 'you']
        }

        u()
      """

  }

  void testSplitEmptyText() {
    shell.evaluate """

        @Typed
        def u() {
          def text = ""
          def splitted = text.split()
          assert splitted == []
        }

        u()
      """

  }

  void testReadLines() {
    shell.evaluate """

        @Typed
        def u() {
          assert "a\\nb".readLines() == ['a', 'b']
          assert "a\\rb".readLines() == ['a', 'b']
          assert "a\\r\\nb".readLines() == ['a', 'b']
          assert "a\\n\\nb".readLines() == ['a', '', 'b']
        }

        u()
      """
  }

  void testReplace() {
    shell.evaluate """

        @Typed
        def u() {
          assert "".replace("", "") == ""
          assert "".replace("", "r") == "r"
          assert "a".replace("", "r") == "rar"
          assert "a".replace("b", "c") == "a"
          assert "a".replace("a", "c") == "c"
          assert "aa".replace("a", "c") == "cc"
          assert "ab".replace("b", "c") == "ac"
          assert "ba".replace("b", "c") == "ca"
          assert "aaa".replace("b", "c") == "aaa"
          assert "aaa".replace("a", "c") == "ccc"
          assert "aba".replace("b", "c") == "aca"
          assert "baa".replace("b", "c") == "caa"
          assert "aab".replace("b", "c") == "aac"
          assert "aa.".replace(".", "c") == "aac"
          assert 'aba'.replace('b', '\$') == 'a\$a'
          assert 'aba'.replace('b', '\\\\') == 'a\\\\a'
          assert 'a\\\\a'.replace('\\\\', 'x') == 'axa'

          assert '\\\\'.replace('\\\\', 'x') == 'x'
          assert '\\\\\\\\'.replace('\\\\', 'x') == 'xx'
          assert '\\\\z\\\\'.replace('\\\\', 'x') == 'xzx'
          assert 'a\\\\\\\\Ea'.replace('\\\\', 'x') == 'axxEa'
          assert '\\\\Qa\\\\\\\\Ea'.replace('\\\\', '\$') == '\$Qa\$\$Ea'
          assert 'a\\\\((z))\\\\Qa'.replace('\\\\', 'x') == 'ax((z))xQa'

          assert (/\\Q\\E\\\\\\Q\\E/).replace(/\\Q\\E\\\\\\Q\\E/, 'z') == 'z'
        }
        u()
      """

  }

  void testNormalize() {
    shell.evaluate """

          @Typed
          def u() {
            assert "a".normalize() == "a"
            assert "\\n".normalize() == "\\n"
            assert "\\r".normalize() == "\\n"
            assert "\\r\\n".normalize() == "\\n"
            assert "a\\n".normalize() == "a\\n"
            assert "a\\r".normalize() == "a\\n"
            assert "a\\r\\n".normalize() == "a\\n"
            assert "a\\r\\n\\r".normalize() == "a\\n\\n"
            assert "a\\r\\n\\r\\n".normalize() == "a\\n\\n"
            assert "a\\nb\\rc\\r\\nd".normalize() == "a\\nb\\nc\\nd"
            assert "a\\n\\nb".normalize() == "a\\n\\nb"
            assert "a\\n\\r\\nb".normalize() == "a\\n\\nb"
          }

          u()
        """
  }

  void testDenormalize() {
    shell.evaluate """

          @Typed
          def u() {
            def LS = System.getProperty('line.separator')
            assert "\\n".denormalize() == LS
            assert "\\r".denormalize() == LS
            assert "\\r\\n".denormalize() == LS
            assert "a\\n".denormalize() == "a\${LS}"
            assert "a\\r".denormalize() == "a\${LS}"
            assert "a\\r\\n".denormalize() == "a\${LS}"
            assert "a\\r\\n\\r".denormalize() == "a\${LS}\${LS}"
            assert "a\\r\\n\\r\\n".denormalize() == "a\${LS}\${LS}"
            assert "a\\nb\\rc\\r\\nd".denormalize() == "a\${LS}b\${LS}c\${LS}d"
            assert "a\\n\\nb".denormalize() == "a\${LS}\${LS}b"
            assert "a\\n\\r\\nb".denormalize() == "a\${LS}\${LS}b"
            assert 'a\\nb\\r\\nc\\n\\rd'.denormalize() == "a\${LS}b\${LS}c\${LS}\${LS}d"
          }

          u()
        """
  }

  void testNormalizationFileRoundTrip() {
    shell.evaluate """
        @Typed
        void innerNormalizationFileRoundTrip(String s) {
            def f = File.createTempFile("groovy.StringTest", ".txt")

            def sd = s.denormalize()
            f.write(sd)
            assert sd == f.text

            f.write(s);
            assert s == f.text

            def rt = (s.denormalize()).normalize()
            assert s.normalize() == rt

            if (!s.contains('\\r')) assert s == rt
        }

        @Typed(debug=true)
        void doNormalizationFileRoundTrip(String s) {
            
            def arr = [s, s.replace('\\n', '\\r'), s.replace('\\n', '\\r\\n'), s.replace('\\n', '\\n\\n')];
            arr.each {
                innerNormalizationFileRoundTrip((String)it)
                innerNormalizationFileRoundTrip(((String)it).reverse())
            }
        }

        @Typed
        def u() {
          doNormalizationFileRoundTrip("a line 1\\nline 2")
          doNormalizationFileRoundTrip("a line 1\\nline 2\\n")
          doNormalizationFileRoundTrip("")
          doNormalizationFileRoundTrip("\\n")
          doNormalizationFileRoundTrip("a")
          doNormalizationFileRoundTrip("abcdef")
          doNormalizationFileRoundTrip("a\\n")
          doNormalizationFileRoundTrip("abcdef\\n")

        }

        u()
      """
  }

  void testSplitEqualsTokenize() {
    shell.evaluate """

          @Typed
          def u() {
            def text = '''
              A text with different words and
              numbers like 3453 and 3,345,454.97 and
              special characters %&)( and also
              everything mixed together 45!kw?
            '''
            def splitted = Arrays.asList(text.split())
            def tokenized = text.tokenize()
            assert splitted == tokenized

          }
          u()
        """
  }
}
