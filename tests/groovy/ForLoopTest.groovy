package groovy

import groovy.CompileTestSupport
import static groovy.CompileTestSupport.shouldCompile
import static groovy.CompileTestSupport.shouldNotCompile

class ForLoopTest extends GroovyShellTestCase {

  def x

  void testFinalParameterInForLoopIsAllowed() {
    // only 'final' should be allowed: other modifiers like 'synchronized' should be forbidden
    shouldNotCompile """
          @Typed
          def u() {
            def collection = ["a", "b", "c", "d", "e"]
            for (synchronized String letter in collection) { }
          }
          u();
        """

    // only 'final' allowed, and no additional modifier
    shouldNotCompile """
          @Typed
          def u() {
            def collection = ["a", "b", "c", "d", "e"]
            for (final synchronized String letter in collection) { }
          }
        """

    shouldCompile """
          @Typed
          def u() {
            def collection = ["a", "b", "c", "d", "e"]
            for (final String letter in collection) { }
            for (final String letter : collection) { }
            for (final letter in collection) { }
            for (final letter : collection) { }
          }
        """
  }

  void testRange() {
    def res = shell.evaluate("""
        @Typed
        def u() {
          def x = 0

          for (i in 0..9) {
              x = x + i
          }

          return x
        }
        u();
      """
    )
    assertEquals(45, res);
  }

  void testRangeWithType() {
    def res = shell.evaluate("""
        @Typed
        def u() {
          def x = 0

          for (Integer i in 0..9) {
              assert i.getClass() == Integer
              x = x + i
          }

          return x;
        }
        u();
      """
    )
    assertEquals(45, res);
  }

  void testRangeWithJdk15StyleAndType() {
    def res = shell.evaluate("""
        @Typed
        def u() {
          def x = 0

          for ( Integer i : 0..9 ) {
              assert i.getClass() == Integer
              x = x + i
          }

          return x;
        }
        u();
      """
    )
    assertEquals(45, res);
  }

  void testList() {
    def res = shell.evaluate("""
        @Typed
        def u() {
          def x = 0

          for (i in [0, 1, 2, 3, 4]) {
              x = x + (int)i
          }

          return x
        }
        u();
      """
    )

    assertEquals(10, res);
  }

  void testArray() {
    def res = shell.evaluate("""
        @Typed
        def u() {
          def array = (0..4).toArray()
          def x = 0

          for (i in array) {
              x = x + (int)i
          }

          return x;
        }
        u();
      """
    )

    assertEquals(10, res);

  }

  void testString() {

    def res = shell.evaluate("""
        @Typed
        def u(List v) {
          def text = "abc"

          for (c in text) {
              v << c
          }
          return v;
        }
        u([]);
      """
    )
    assertEquals(["a", "b", "c"], res);
  }

  void testVector() {
    def res = shell.evaluate("""
        @Typed
        def u(List v) {
          def vector = new Vector()
          vector.addAll([1, 2, 3])

          for (i in vector.elements()) {
              v << i
          }
          return v;
        }
        u([]);
      """
    )
    assertEquals([1, 2, 3], res);

  }

  void testClassicFor() {
    shell.evaluate("""
        @Typed
        def u() {
          def sum = 0
          for (int i = 0; i < 10; i++) {
              sum++
          }
          assert sum == 10

          def list = [1, 2]
          sum = 0
          for (Iterator i = list.iterator(); i.hasNext();) {
              sum = sum + (int)i.next()
          }
          assert sum == 3
        }
        u();
      """
    )
  }

  void testClassicForNested() {
    shell.evaluate("""
        @Typed
        def u() {
          def sum = 0
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    sum++
                }
            }
            assert sum == 100
        }
        u();
      """
    )

  }

  @Typed
  void testClassicForWithContinue() {
    shell.evaluate("""
        @Typed
        def u() {
          def sum1 = 0
          for (int i = 0; i < 10; i++) {
              if (i % 2 == 0) continue
              sum1 = sum1 + i
          }
          assert sum1 == 25

          // same as before, but with label
          def sum2 = 0
          test:
          for (int i = 0; i < 10; i++) {
              if (i % 2 == 0) continue test
              sum2 = sum2 + i
          }
          assert sum2 == 25
        }
        u();
      """
    )
  }

  void testClassicForWithEmptyInitializer() {
    shell.evaluate("""
        @Typed
        def u() {
          def i = 0
          def sum1 = 0
          for (; i < 10; i++) {
              if (i % 2 == 0) continue
              sum1 = sum1 + i
          }
          assert sum1 == 25
        }
        u();
      """
    )
  }

  void testClassicForWithEmptyBody() {
    shell.evaluate("""
        @Typed(debug=true)
        def u() {
          int i
          for (i = 0; i < 5; i++);
          assert i == 5
        }
        u();
      """
    )
  }

  void testClassicForWithEverythingInitCondNextExpressionsEmpty() {
    shell.evaluate("""
        @Typed
        def u() {
          int counter = 0
          for (;;) {
              counter++
              if (counter == 10) break
          }

          assert counter == 10, "The body of the for loop wasn't executed, it should have looped 10 times."
        }
        u();
      """
    )

  }
}
