package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

public class Issue195Test extends GroovyShellTestCase {
    void testClassImplmentingAnotherClass() {
        shouldNotCompile("""
            @Typed package p
            
            class A195 implements B195 {}
            
            class B195 {}
        """,
        "You are not allowed to implement the class")
    }
}