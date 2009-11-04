package groovy.util

@Typed
public class EachTest extends GroovyShellTestCase {

    void testIterator () {
        def res = []
        ((List<Integer>)[1,2,3]).iterator().each { int it ->
            res << it + 1
        }
        assertEquals ([2,3,4], res)
    }

    void testIteratorDefType () {
        def res = shell.evaluate("""
        @Typed u () {
            def res = []
            ((List<Integer>)[1,2,3]).iterator().each {
                res << it + 1
            }
            res
        }
        u ()
        """)
        assertEquals ([2,3,4], res)
    }

    void testIteratorWithState () {
        def res = []
        res << ((List<Integer>)[1,2,3,4]).iterator().each(0) { int it, int state ->
            res << (state += it)
            state
        }
        assertEquals ([1,3,6,10,10], res)
    }

    void testCollection () {
        def res = []
        ((List<Integer>)[1,2,3]).each { int it ->
            res << it + 1
        }
        assertEquals ([2,3,4], res)
    }

    void testCollectionWithState () {
        def res = []
        res << ((List<Integer>)[1,2,3,4]).each(0) { int it, int state ->
            res << (state += it)
            state
        }
        assertEquals ([1,3,6,10,10], res)
    }

    void testArray () {
        def res = []
        ((Integer[])[1,2,3]).each { int it ->
            res << it + 1
        }
        assertEquals ([2,3,4], res)
    }

    void testArrayWithState () {
        def res = []
        res << ((Integer[])[1,2,3,4]).each(0) { int it, int state ->
            res << (state += it)
            state
        }
        assertEquals ([1,3,6,10,10], res)
    }


    void testMapKeyValue () {
        def res = []
        ((Map<String,Integer>)[a:20, b:40]).eachKeyValue { String key, int value ->
            res << key
            res << value
        }
        assertEquals (["a", 20, "b", 40], res)
    }

    void testMapKeyValueWithState () {
        def res = []
        ((Map<String,Integer>)[a:20, b:40]).eachKeyValue(0) { String key, int value, int state ->
            res << key
            res << value
            res << state
            res.size()
        }
        assertEquals (["a", 20, 0, "b", 40, 3], res)
    }

    void testIteratorWithIndex () {
        def res = []
        ((List<Integer>)[1,2,3]).iterator().eachWithIndex { int it, int index ->
            res << it + index
        }
        assertEquals ([1,3,5], res)
    }

    void testIteratorWithStateWithIndex () {
        def res = []
        res << ((List<Integer>)[1,2,3,4]).iterator().eachWithIndex(0) { int it, int state, int index ->
            res << (state += it + index)
            state
        }
        assertEquals ([1,4,9,16,16], res)
    }

    void testCollectionWithIndex () {
        def res = []
        ((List<Integer>)[1,2,3]).eachWithIndex { int it, int index ->
            res << it + index
        }
        assertEquals ([1,3,5], res)
    }

    void testCollectionWithStateWithIndex () {
        def res = []
        res << ((List<Integer>)[1,2,3,4]).eachWithIndex(0) { int it, int state, int index ->
            res << (state += it + index)
            state
        }
        assertEquals ([1,4,9,16,16], res)
    }

    void testArrayWithIndex () {
        def res = []
        ((Integer[])[1,2,3]).eachWithIndex { int it, int index ->
            res << it + index
        }
        assertEquals ([1,3,5], res)
    }

    void testArrayWithStateWithIndex () {
        def res = []
        res << ((Integer[])[1,2,3,4]).eachWithIndex(0) { int it, int state, int index ->
            res << (state += it + index)
            state
        }
        assertEquals ([1,4,9,16,16], res)
    }

    void testMapKeyValueWithIndex () {
        def res = []
        ((Map<String,Integer>)[a:20, b:40]).eachKeyValueWithIndex { String key, int value, int index ->
            res << key
            res << value
            res << index
        }
        assertEquals (["a", 20, 0, "b", 40, 1], res)
    }

    void testMapKeyValueWithStateWithIndex () {
        def res = []
        ((Map<String,Integer>)[a:20, b:40]).eachKeyValueWithIndex(0) { String key, int value, int state, int index ->
            res << key
            res << value
            res << state
            res << index
            res.size()
        }
        assertEquals (["a", 20, 0, 0, "b", 40, 4, 1], res)
    }
}