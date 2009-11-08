package groovy.util

@Typed
class MappersTest extends GroovyShellTestCase {
    void testPrimitive () {
        def toTransform = /*(int[])*/[0, 1, 2]         //todo: restore array
        /*assertTrue (toTransform instanceof int[])*/
        def res = toTransform.map { it.toString () }
        assertEquals (["0", "1", "2"], res )
    }

    void testCollection () {
        def toTransform = [0, 1, 2]
        def res = toTransform.map { it.toString () }
      assertEquals (["0", "1", "2"], res )
    }

//    void testArray () {
//        def toTransform = (Integer[])[0, 1, 2]
//        def res = toTransform.transform { int v ->
//           v.toString ()
//        }
//        assertEquals (["0", "1", "2"], res )
//    }
//
    void testIterator () {
        def iter = [0, 1, 2].iterator().map { it.toString() }
        def res = []
        while (iter.hasNext()) {
            res << iter.next ().toUpperCase()
        }
        assertEquals (["0", "1", "2"], res )
    }

    void testProduct () {
        [0,1,2,3,4].iterator().product{
            println it + 10
            [0,1,2,3].iterator ()
        }.each { pair ->
            println "${pair.first} ${pair.second}"
            // @todo followind line fails
//            println "${pair.first} ${pair.second} ${pair.first + pair.second}"
        }
    }
}