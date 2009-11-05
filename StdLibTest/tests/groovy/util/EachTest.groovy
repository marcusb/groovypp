package groovy.util

@Typed
public class EachTest extends GroovyShellTestCase {

    void testIterator () {
        def res = []
        ((List<Integer>)[1,2,3]).iterator().each {
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

    void testIteratorIntType () {
        def res = shell.evaluate("""
        @Typed u () {
            def res = []
            ((List<Integer>)(0..100000)).iterator().each {
                res << it + 1
            }
            res
        }
        u ()
        """)
        assertEquals ((1..100001), res)
    }

    void testIteratorWithState () {
        def res = []
        res << ((List<Integer>)[1,2,3,4]).iterator().each {
            @Field int state = 0
            res << (state += it)
            state
        }
        assertEquals ([1,3,6,10,10], res)
    }

    void testCollection () {
        def res = []
        ((List<Integer>)[1,2,3]).each {
            res << it + 1
        }
        assertEquals ([2,3,4], res)
    }

    void testCollectionWithState () {
        def res = []
        res << ((List<Integer>)[1,2,3,4]).each {
            assert this instanceof Function1
            @Field int state = 0
            res << (state += it)
            state
        }
        assertEquals ([1,3,6,10,10], res)
    }

    void testCollectionWithStateCompile () {
        shell.evaluate """
@Typed(debug=true)
def u () {
        def res = []
        res << ((List<Integer>)[1,2,3,4]).each { int it ->
            assert this instanceof Function1
            @Field int state = 0
            res << (state += it)
            state
        }
        assert [1,3,6,10,10] == res
}
u()
        """
    }

    void testArray () {
        def res = []
        ((Integer[])[1,2,3]).each {
            res << it + 1
        }
        assertEquals ([2,3,4], res)
    }

    void testArrayWithState () {
        def res = []
        res << ((Integer[])[1,2,3,4]).each {
            @Field int state = 0
            res << (state += it)
            state
        }
        assertEquals ([1,3,6,10,10], res)
    }


    void testMapKeyValue () {
        def res = []
        ((Map<String,Integer>)[a:20, b:40]).each { key, value ->
            res << key
            res << value
        }
        assertEquals (["a", 20, "b", 40], res)
    }
}