package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

public class Issue50Test extends GroovyShellTestCase {
    void testBug () {
        shouldNotCompile """
            @Typed package p
            class U { void u() {} }
            class V { void v() {} }

            class C {
              def f(U u) {}
              def f(V v) {}
              def foo() {
                f([u : {}])
              }
            }

            new C().foo()
        """
    }
}