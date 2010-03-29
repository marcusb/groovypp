package org.mbte.groovypp.compiler.Issues

public class Issue205Test extends GroovyShellTestCase {
    void testExceptionPropogationOfCompilationError() {
        try {
            new GroovyShell().parse """
                @Typed
                class CloneTest extends GroovyTestCase {
                    List numbers = [1, 2]
                    void testClone() {
                        def newNumbers = ((ArrayList)numbers).clone()
                    }
                }
            """
            fail("The code should not compile because static compiler identified List to ArrayList as an impossible cast")
        } catch (ex) {
            assert ex.message.contains('Impossible cast')
        }
    }
}