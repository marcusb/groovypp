package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

@Typed
public class Issue135Test extends GroovyShellTestCase {
    void testMe () {
        shouldNotCompile """
@Typed class Test{
    static setFoo(String x) {}
    static f() {
        Test.foo
    }
}
Test.f()
        """
    }

  void testSetterOK () {
        shell.evaluate """
@Typed class Test{
    static void setFoo(String x) {}
    static f() {
        Test.foo = ""
    }
}
Test.f()
        """
    }
}