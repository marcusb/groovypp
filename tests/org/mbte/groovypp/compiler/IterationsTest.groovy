package org.mbte.groovypp.compiler

public class IterationsTest extends GroovyShellTestCase {
    void testSimple () {
        def res = shell.evaluate("""
            @Typed(debug=true)
            u () {
                [0,1,2,3,4,5].findAll { int it ->
                   it % 2 == 1
                }
            }
            u ()
        """)
        assertEquals ([1,3,5], res)
    }
}