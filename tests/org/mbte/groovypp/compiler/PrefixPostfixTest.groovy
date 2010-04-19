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

public class PrefixPostfixTest extends GroovyShellTestCase {
  void testPostfixVar() {
    ["Number", char, "int", "Integer", "byte", "Byte",
            "short", "Short", "long", "Long", "float", "Float", "double", "Double",
            "Character"].each {

      println it

      def res = shell.evaluate("""
          @Typed
          def u ($it val, List res) {
            while(val) {
              res.add(val--)
            }
            res
          }

            u (($it)5,[])
            """
      )

      println res
      assertEquals([5, 4, 3, 2, 1], res.collect {jt -> (int) jt })
    }
  }

  void testPostfixProp() {
    ["static",""].each {st ->
      ["Number", int, "Integer", "byte", "Byte",
              "short", "Short", "long", "Long", "float", "Float", "double", "Double",
              "char", "Character"].each {

        println it

        def res = shell.evaluate("""
              class U$st {
                $st $it prop
              }

              @Typed
              def u (U$st u, List res) {
                while(u.prop) {
                  res.add(u.prop--)
                }
                res
              }

                u (new U$st(prop:($it)5),[])
                """
        )

        println res
        assertEquals([5, 4, 3, 2, 1], res.collect {jt -> (int) jt })
      }
    }
  }

    void testPostfixField() {
      ["static", ""].each {st ->
        ["int", "Integer", "byte", "Byte",
                "short", "Short", "long", "Long", "float", "Float", "double", "Double",
                "char", "Character", "Number"].each {

          println it

          def res = shell.evaluate("""
                class U {
                  public $st $it prop
                }

                @Typed
                def u (U u, List res) {
                  while(u.prop) {
                    res.add(u.prop--)
                  }
                  res
                }

                  u (new U(prop:($it)5),[])
                  """
          )

          println res
          assertEquals([5, 4, 3, 2, 1], res.collect {jt -> (int) jt })
        }
      }
    }

  void testPrefixProp() {
    ["", "static"].each {st ->
      ["int", "Integer", "byte", "Byte",
              "short", "Short", "long", "Long", "float", "Float", "double", "Double",
              "char", "Character", "Number"].each {

        println it

        def res = shell.evaluate("""
            import org.mbte.groovygrid.compile.*
              class U {
                $st $it prop
              }

              @Typed
              def u (U u, List res) {
                while(u.prop) {
                  res.add(--u.prop)
                }
                res
              }

                u (new U(prop:($it)5),[])
                """
        )

        println res
        assertEquals([4, 3, 2, 1, 0], res.collect {jt -> (int) jt })
      }
    }
  }

    void testPrefixField() {
      ["", "static"].each {st ->
        ["int", "Integer", "byte", "Byte",
                "short", "Short", "long", "Long", "float", "Float", "double", "Double",
                "char", "Character", "Number"].each {

          println it

          def res = shell.evaluate("""
              import org.mbte.groovygrid.compile.*
                class U {
                  public $st $it prop
                }

                @Typed
                def u (U u, List res) {
                  while(u.prop) {
                    res.add(--u.prop)
                  }
                  res
                }

                  u (new U(prop:($it)5),[])
                  """
          )

          println res
          assertEquals([4, 3, 2, 1, 0], res.collect {jt -> (int) jt })
        }
      }
    }

  void testPostfixArr() {
    ["Number", int, "Integer", "byte", "Byte",
            "short", "Short", "long", "Long", "float", "Float", "double", "Double",
            "char", "Character"].each {

      println it

      def res = shell.evaluate("""
    @Typed
    def U ($it[] val, List res) {
      while(val[0]) {
        res.add(val[0]--)
      }
      res
    }

      U ([($it)5] as $it[],[])
      """
      )

      println res
      assertEquals([5, 4, 3, 2, 1], res.collect {jt -> (int) jt })
    }
  }

    void testPostfixList() {
        ["Number", "Integer", "Byte",
                "Short", "Long", "Float", "Double",
                "Character"].each {

        println it

        def res = shell.evaluate("""
      @Typed
      def U (List<$it> val, List res) {
        while(val[0]) {
          res.add(val[0]--)
        }
        res
      }

        U ([($it)5] as List<$it>,[])
        """
        )

        println res
        assertEquals([5, 4, 3, 2, 1], res.collect {jt -> (int) jt })
      }
    }

  void testPrefixVar() {
    ["int", "Integer", "byte", "Byte",
            "short", "Short", "long", "Long", "float", "Float", "double", "Double",
            "char", "Character", "Number"].each {

      println it

      def res = shell.evaluate("""
          @Typed
          def u ($it val, List res) {
            while(val) {
              res.add(--val)
            }
            res
          }

            u (($it)5,[])
            """
      )

      println res
      assertEquals([4, 3, 2, 1, 0], res.collect {jt -> (int) jt })
    }
  }

  void testPrefixArr() {
    ["Number", int, "Integer", "byte", "Byte",
            "short", "Short", "long", "Long", "float", "Float", "double", "Double",
            "char", "Character"].each {

      println it

      def res = shell.evaluate("""
      @Typed
      def U ($it[] val, List res) {
        while(val[0]) {
          res.add(--val[0])
        }
        res
      }

        U ([($it)5] as $it[],[])
        """
      )

      println res
      assertEquals([4, 3, 2, 1, 0], res.collect {jt -> (int) jt })
    }
  }

    void testPrefixList() {
      ["Number", "Integer", "Byte",
              "Short", "Long", "Float", "Double",
              "Character"].each {

        println it

        def res = shell.evaluate("""
        @Typed
        def U (List<$it> val, List res) {
          while(val[0]) {
            res.add(--val[0])
          }
          res
        }

          U ([($it)5] as List<$it>,[])
          """
        )

        println res
        assertEquals([4, 3, 2, 1, 0], res.collect {jt -> (int) jt })
      }
    }

  void testVar() {
    def types = ["Number", int, "Integer", "byte", "Byte",
            "short", "Short", "long", "Long", "float", "Float", "double", "Double",
            "char", "Character", "BigInteger", "BigInteger"]

    types.each {jt ->
      types.each {it ->
        println "$it, $jt"

        def res = shell.evaluate("""
      @Typed
      def U (List res) {
        $it val = ($jt)5
        while(val) {
          res.add(--val)
        }
        res.collect{ kt -> (int) kt }
      }

        U ([])
        """
        )

        println res
        assertEquals([4, 3, 2, 1, 0], res)
      }
    }
  }

  void testStaticFieldPrefixPostfix() {
    def res = shell.evaluate("""
    class A {
      static int FIELD = 0
      @Typed
      public static def foo ()
      {
        FIELD++
        --FIELD
      }
    }
    A.foo()
    """)
    assertEquals 0, res
  }
}