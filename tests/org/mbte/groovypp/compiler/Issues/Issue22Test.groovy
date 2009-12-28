package org.mbte.groovypp.compiler.Issues

public class Issue22Test extends GroovyShellTestCase {
    void testFail () {
        shell.evaluate """
        @Typed def foo () {
           Reference r = [1]
           r.get().intValue()
        }
        foo()    
        """
    }
}