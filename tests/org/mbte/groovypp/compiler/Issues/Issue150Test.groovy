package org.mbte.groovypp.compiler.Issues

public class Issue150Test extends GroovyShellTestCase {
  void testStackOverflowErrorOnSuperCall () {
        shell.evaluate """
            @Typed package test
            
            class Test{
                static main(args) {
                    Foo foo = new Foo()
                    String str = foo.toString()
                    assert str.startsWith("test.Foo")
                    assert str.endsWith("Foo()")
                }
            }
            
            class Foo {
                def name
                String toString() {
                    return super.toString() + ", Foo()"
                }
            }
        """
    }
}