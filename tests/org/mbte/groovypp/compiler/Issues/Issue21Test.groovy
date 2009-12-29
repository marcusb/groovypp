package org.mbte.groovypp.compiler.Issues

public class Issue21Test extends GroovyShellTestCase {
    void testFail () {
        shell.evaluate """
        @Typed public static void foo ()
        {
          sleep( 1000 )
        }
        foo()
        """
    }
}