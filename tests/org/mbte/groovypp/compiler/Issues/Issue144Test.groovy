package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue144Test extends GroovyShellTestCase {
  void test144 () {
        shell.evaluate """
@Typed
class Test {
    Closure foo = {"foo"}
    static f() {
        Test t = new Test()
        assert t.foo() == "foo"
    }
}
Test.f()
        """
    }
}