package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue141142Test extends GroovyShellTestCase {
  void test141 () {
        shell.evaluate """
@Typed class Test {
    static f() {
        Reference count = [2]
        def c = { count =  count ** 3 }
        c()
        assert count == 8
    }
}
Test.f()
        """
    }

  void test142 () {
        shell.evaluate """
@Typed class Test {
    static f() {
        Reference count = [2]
        def c = { count =  count + 1.0 }
        c()
        assert count == 3
    }
}
Test.f()
        """
    }
}