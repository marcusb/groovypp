package org.mbte.groovypp.compiler

public class MapAsObjectTest extends GroovyShellTestCase {
    void testMe () {
        def res = shell.evaluate ("""
            @Trait
            abstract class Function1<T,R> {
                abstract R apply (T param)

                R getAt (T arg) {
                    apply(arg)
                }
            }

            @Typed(debug=true)
            <T,R> Iterator<R> map (Iterator<T> self, Function1<T,R> op) {
                [next: { op [ self.next() ] }, hasNext: { self.hasNext() }, remove: { self.remove() } ]
            }

            @Typed
            def u () {
                def res = []

                def newIt = map([1,2,3,4].iterator ()) {
                    it + 10
                }

                while (newIt.hasNext ())
                    res << (newIt.next () + 1)

                res
            }

            u ()
        """)

       assertEquals ([12, 13, 14, 15], res) 
    }
}