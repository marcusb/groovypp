package org.mbte.groovypp.compiler

public class FieldAnnotationTest extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate("""
@Typed
def u (s) {
   @Field def res = []
   res << s
}

def res = u(1)
u(2)
u(3)

assert [1,2,3] == res 
        """)
    }
}