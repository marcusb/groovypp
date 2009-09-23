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

      @Compile
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
      @Compile
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

      @Compile(debug=true)
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

      @Compile(debug=true)
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

        @Compile(debug=true)
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

        @Compile(debug=true)
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

        @Compile(debug=true)
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

          @Compile(debug=true)
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

          @Compile
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