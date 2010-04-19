package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

@Typed
public class Issue149Test extends GroovyShellTestCase {
    void testMe () {
        shouldNotCompile("""
@Typed package test

class A {
    def foo() {}
}

class B extends A {
    private foo() {}
}
""", "Attempting to assign weaker access to B.foo()")
    }
}