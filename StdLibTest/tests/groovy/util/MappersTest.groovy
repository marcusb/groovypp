package groovy.util

import com.sun.corba.se.spi.orbutil.threadpool.ThreadPool
import java.util.concurrent.Executors

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
        (0..<10).iterator().product{
            println (-10 + it + 10) // to make sure it is integer
            (0..it).iterator ()
        }.each {
            Pair<Pair,Pair> p = [it, it]
            println "${p.first.first} ${p.second.second}"
            println "${it.first} ${it.second} ${it.first + it.second}"
        }
    }

    void testZip () {
      def range = 0..2
      def zipped = range.iterator().zip(range.map {it + 1}.iterator())
      def list = []
      while (zipped.hasNext()) list << zipped.next()
      assertEquals((List<Pair>) [[0, 1], [1, 2], [2, 3]], list)
    }

    void testFlatMap () {
      def l = [[0,1,2], [3,4]]
      assertEquals(["0", "1", "2", "3", "4"], l.flatMap{it.toString()}.asList())
    }

    void testFlatten () {
      def l = [[0,1,2], [3,4]]
      assertEquals([0, 1, 2, 3, 4], l.flatten().asList())
    }

    void testMapConcurrently () {
        def res = (0..100000).iterator ().mapConcurrently (Executors.newFixedThreadPool(10), 50) {
            println "${Thread.currentThread().id} $it"
            it + 1
        }.toList ()
        assertEquals ((1..100001), res)
    }
}