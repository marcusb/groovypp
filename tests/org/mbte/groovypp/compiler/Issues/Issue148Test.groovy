package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue148Test extends GroovyShellTestCase {
  void testMe () {
        shell.evaluate """
@Typed class Test{
    static main(args) {
       Helper h = new Helper()
       [1].each(h.&foo)
    }
}

class Helper {
    def foo(x) {}
}
        """
    }
}