package org.mbte.groovypp.compiler

public class FieldAnnotationTest extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate("""
@Typed(debug=true)
def u () {
        def res = []
        [1,2,3,4].iterator().each {
            @Field int state = 0
            res << (state += it)
        }
        res
}

assert [1,3,6,10] == u()
        """)
    }
}