package groovy.util.concurrent

@Typed
public class FVectorTest extends GroovyTestCase {
    void testInsert () {
        FVector<Integer> vec = FVector.emptyVector

        for(i in 0..<1000000) {
            vec = vec + i
        }

        assertEquals 1000000, vec.length
    }
}