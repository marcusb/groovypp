package groovy.util

@Typed
public class ClosureWithStateTest extends GroovyTestCase {
    void testMe() {
      def sum = [1, 2, 3].each(0){int it, int sum -> sum += it}
      assertEquals(6, sum)
    }
}