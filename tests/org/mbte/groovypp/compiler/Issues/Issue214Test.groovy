package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue214Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed package p

class Foo {
  def foo(msg) {msg}
}

def z = new Foo() {
    def foo( msg) {
        super.foo(msg)
    }
}
assert z.foo("42") == "42"
        """
    }
}