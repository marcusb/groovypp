package groovy.util.concurrent

@Typed
public class FVectorTest extends GroovyTestCase {
    void testAddRemove () {
        FVector<Integer> vec = FVector.emptyVector

        for(i in 0..<1000000) {
          vec = vec + i
          if (i % 2) {
            def p = vec.pop()
            assertEquals i, p.first
            vec = p.second
          }
        }

        assertEquals 500000, vec.length

        for(i in 0..<500000) {
          assertEquals 2*i, vec[i]
        }
    }

    void testIterator() {
      FVector<Integer> vec = FVector.emptyVector
      def range = 0..<100000
      for(i in range) {
          vec = vec + i
      }

      def l = []
      for (i in vec) {
        l << i
      }

      assertEquals range, l
    }
}