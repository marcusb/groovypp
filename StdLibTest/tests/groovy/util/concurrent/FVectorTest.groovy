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
    }
}