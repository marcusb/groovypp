package groovy.util

@Typed
public class ClosureWithStateTest extends GroovyShellTestCase {
    void testCompile() {
      shell.evaluate("""
          @Typed
          def u () {
              def sum = [1, 2, 3].each(0){int it, int sum -> sum + it}
              assert 6==sum
          }
          u ()
      """)
    }

//    void testRun() {
//      def sum = [1, 2, 3].each(0){int it, int sum -> sum + it}
//      assertEquals(6, sum)
//    }
}