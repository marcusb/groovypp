/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package groovy.util

@Typed
public class EachTest extends GroovyShellTestCase {

    void testIterator () {
        def res = []
        [1,2,3].iterator().each {
            res << it + 1
        }
        assertEquals ([2,3,4], res)
    }

    void testIteratorDefType () {
        def res = shell.evaluate("""
        @Typed u () {
            def res = []
            [1,2,3].iterator().each {
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
            ((List<Integer>)(0..100000)).iterator().each { int it ->
                res << it + 1
                println it
            }
            res
        }
        u ()
        """)
        assertEquals ((1..100001), res)
    }

    void testArrayIntType () {
        def res = shell.evaluate("""
        @Typed <T> T u (T [] o = null) {
            def res = []
            ((Integer[])[1,2,3,4]).each {
                res << it + 1
                println it
            }
            res
        }
        u ()
        """)
        assertEquals ((2..5), res)
    }

    void testIteratorWithState () {
        def res = []
        res << [1,2,3,4].iterator().each { int it ->
            @Field int state = 0
            res << (state += it)
            state
        }
        assertEquals ([1,3,6,10,null], res)
    }

    void testCollection () {
        def res = []
        [1,2,3].each { int it ->
            res << it + 1
        }
        assertEquals ([2,3,4], res)
    }

    void testCollectionWithState () {
        def res = []
        res << [1,2,3,4].each { int it ->
            assert this instanceof Function1
            @Field int state = 0
            res << (state += it)
            state
        }
        assertEquals ([1,3,6,10,null], res)
    }

    void testCollectionWithStateCompile () {
        shell.evaluate """
@Typed
def u () {
        def res = []
        res << [1,2,3,4].each {
            assert this instanceof Function1
            @Field int state = 0
            res << (state += it)
            state
        }
        assert [1,3,6,10,null] == res
}
u()
        """
    }

    void testArray () {
        def res = []
        [1,2,3].each { int it ->
            res << it + 1
        }
        assertEquals ([2,3,4], res)
    }

    void testArrayWithState () {
        def res = []
        res << ((Integer[])[1,2,3,4]).each { int it ->
            @Field int state = 0
            res << (state += it)
            state
        }
        assertEquals ([1,3,6,10,null], res)
    }


    void testMapKeyValue () {
        def res = []
        [a:20, b:40].each { key, value ->
            res << key.toUpperCase()
            res << value + 1
        }
        assertEquals (["A", 21, "B", 41], res)
    }

    void testFoldLeftWithMap () {
        shell.evaluate """
            @Typed(debug=true) package p

            def res = [1,2,3,3,2,1].foldLeft (new HashMap<?,Integer>()) { el, map ->
                map[el] = map.get(el,0) + 1
                map
            }
            GroovyTestCase.assertEquals res, [1:2, 2:2, 3:2]
        """
    }
}