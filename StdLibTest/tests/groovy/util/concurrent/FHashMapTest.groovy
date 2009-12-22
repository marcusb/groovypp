package groovy.util.concurrent

@Typed
public class FHashMapTest extends GroovyTestCase {
    void testInsert () {
        def map = FHashMap.emptyMap
        def m = map.put(10,-1).put(2,-2).put(2,-3)
        assertEquals 2, m.size()
        assertEquals (-3,m [2])
    }

    void testInsertMany () {
        FHashMap<Integer,Integer> map = FHashMap.emptyMap

        for(i in 0..<100000) {
            map = map.put(i, -i)
        }
        for(i in 0..<100000) {
            map = map.remove(2*i)
        }
        assertEquals 50000, map.size()
        assertEquals (-25,map [25])
    }
}