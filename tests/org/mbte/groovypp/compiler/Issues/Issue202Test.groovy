package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

@Typed
public class Issue202Test extends GroovyShellTestCase {
    void testMe () {
        shouldNotCompile """
@Typed package p

class A {
    private foo() {"A"}
}

class B extends A {
    private foo() {"B"}
}

A a = new B()
print a.foo()        """
    }
}