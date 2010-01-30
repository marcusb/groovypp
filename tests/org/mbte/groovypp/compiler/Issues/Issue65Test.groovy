package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

public class Issue65Test extends GroovyShellTestCase {
    void testMe () {
        shouldNotCompile """
        @Typed package p
        class C {
          C(int f) { }
        }
        new C()
        """
    }
}