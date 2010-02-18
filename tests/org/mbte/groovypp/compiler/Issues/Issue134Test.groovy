package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

@Typed
public class Issue134Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed package test

class Test {
    Closure foo2 = this.&foo
    def foo() { "foo() called" }

    Closure bar2 = this.&bar
    def bar(arg) { "bar() called with " + arg }
}

Test obj = new Test()
assert obj.foo() == "foo() called"
assert obj.foo2() == "foo() called"

assert obj.bar(0) == "bar() called with 0"
assert obj.bar2(1) == "bar() called with 1"

"""
    }

  void testMultiple () {
    shouldNotCompile """
@Typed package test

class Test {
    Closure foo3 = this.&foo
    def foo() {}
    def foo(arg) {}
}
new Test()
"""
  }
}