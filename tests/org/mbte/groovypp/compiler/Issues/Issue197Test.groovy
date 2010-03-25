package org.mbte.groovypp.compiler.Issues

import org.codehaus.groovy.control.MultipleCompilationErrorsException

public class Issue197Test extends GroovyShellTestCase {
    void testImplicitThisPassingInInstanceInitBlock() {
        try {
            shell.evaluate """
                @Typed
                class Test {
                    {
                        new A()
                        throw new RuntimeException('Inner class instance got created correctly')
                    }
                    class A {}
                }
                new Test()
            """
            fail('Should have failed to indicate success if instance init block got executed as expected')
        } catch (RuntimeException e) {
            assert e.message.contains("Inner class instance got created correctly")
        }
    }
}