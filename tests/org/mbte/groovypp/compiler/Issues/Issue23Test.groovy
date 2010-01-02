package org.mbte.groovypp.compiler.Issues

public class Issue23Test extends GroovyShellTestCase {
    void testFail () {
        shell.evaluate """
        @Typed class C {
        private static final boolean[] BOOLEANS = {
                                                return new boolean[ 256 ];
                                          }()
        }
        new C()    
        """
    }
}