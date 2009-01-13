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

      @CompileStatic
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
      @CompileStatic
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
}