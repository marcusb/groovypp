package org.mbte.groovypp.compiler.Issues

@Typed
class Issue83Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
            @Typed(TypePolicy.MIXED) package p

            def foo() {
                var1 = 1
            }
            foo()
            println var1
        """
    }
}