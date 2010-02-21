package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue147Test extends GroovyShellTestCase {
  void test147 () {
        shell.evaluate """
@Typed class Test{
    static foo() {"called"}

    static main(args) {
        def bar = Test.&foo
        assert bar() == "called"
    }
}
        """
    }
}