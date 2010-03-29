package org.mbte.groovypp.compiler.Issues

public class Issue203Test extends GroovyShellTestCase {
    void test1() {
          shell.evaluate """
@Typed package pppp

class A {
}

@Typed
class B {}

new B()
"""
    }
}