package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

public class Issue71Test extends GroovyShellTestCase {
    void testMe () {
        shouldNotCompile """
        @Typed package p
        class C {
          final def f
          void foo() {
            f = null
          }
        }
        new C().foo()
        """
    }

    void testPrivate () {
        shouldNotCompile """
        @Typed package p
        class C {
          final private def f
          void foo() {
            f = null
          }
        }
        new C().foo()
        """
    }
}