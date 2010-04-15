package org.mbte.groovypp.compiler.Issues

import org.codehaus.groovy.control.MultipleCompilationErrorsException

@Typed
public class Issue221Test extends GroovyShellTestCase {
    void testMe () {
        def s = shell
        def res = shouldFail(MultipleCompilationErrorsException) {
            s.evaluate  """
    @Typed package p

    def classLoader = new GroovyClassLoader ()
    classLoader.parseClass '''
        @Grab(group="roshan", module="dawrani", version="0.0.1")
        class Foo {}
    '''
            """
        }
        assertTrue(res.contains("General error during conversion: Error grabbing Grapes -- [unresolved dependency: roshan#dawrani;0.0.1: not found]"))
    }
}