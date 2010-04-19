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

public class LoopTest extends GroovyShellTestCase {
  void testWhile() {
    def res = shell.evaluate("""
      class Counter {
         int value

         boolean positive () {
            value > 0
         }

         void decrease () {
           value--
         }

         int getValue () {
           value
         }
      }

      @Typed
      def u (Counter val, List res) {
        while(true) {
          if (!val.positive())
            break;
          res.add(val.getValue())
          val.decrease()
        }
        res
      }

        u (new Counter(value:5),[])
        """
    )

    println res
    assertEquals([5, 4, 3, 2, 1], res)
  }

  void testWhileWithDecrement() {
    def res = shell.evaluate("""
      @Typed
      def u (int val, List res) {
        while(val) {
          res.add(val--)
        }
        res
      }

        u (5,[])
        """
    )

    println res
    assertEquals([5, 4, 3, 2, 1], res)
  }

  void testForWithCollection() {
    def res = shell.evaluate("""

      @Typed
      def u (List res) {
        for ( i in 0..4 ) {
            res.add(i);
        }
        res
      }

        u ([])
        """
    )

    println res
    assertEquals([0, 1, 2, 3, 4], res)
  }

  void testForWithClosuresNormal() {
    def res = shell.evaluate("""

      @Typed
      def u (List res) {
        for (int i = 0; i < 5; i++) {
          res.add(i);
        }
        res
      }

        u ([])
        """
    )

    assertEquals([0, 1, 2, 3, 4], res)
  }

  void testForWithClosuresNoInitBlock() {
    def res = shell.evaluate("""

        @Typed
        def u (List res) {
          int i = 0;
          for (; i < 5; i++) {
            res.add(i);
          }
          res
        }

          u ([])
          """
    )

    assertEquals([0, 1, 2, 3, 4], res)
  }

  void testForWithClosuresNoBinaryBlock() {
    def res = shell.evaluate("""

        @Typed
        def u (List res) {
          for (int i = 0; ; i++) {
            if (i > 4)
              break;
            res.add(i);

          }
          res
        }

          u ([])
          """
    )

    assertEquals([0, 1, 2, 3, 4], res)
  }

  void testForWithClosuresNoIncrementBlock() {
    def res = shell.evaluate("""

        @Typed
        def u (List res) {
          for (int i = 0; i < 5;) {
            res.add(i);
            i++;
          }
          res
        }

          u ([])
          """
    )

    assertEquals([0, 1, 2, 3, 4], res)
  }

  void testForWithClosuresEmpty() {
    def res = shell.evaluate("""

          @Typed
          def u (List res) {
            int i = 0;
            for (;;) {
              if (i > 4)
                break;
              res.add(i);
              i++;
            }
            res
          }

            u ([])
            """
    )

    assertEquals([0, 1, 2, 3, 4], res)
  }

  void testForWithClosuresVoidIncrement() {
    def res = shell.evaluate("""

          @Typed
          def u (List res) {
            int i = 0;
            for (;;println("increment")) {
              if (i > 4)
                break;
              res.add(i);
              i++;
            }
            res
          }

            u ([])
            """
    )

    assertEquals([0, 1, 2, 3, 4], res)
  }

}