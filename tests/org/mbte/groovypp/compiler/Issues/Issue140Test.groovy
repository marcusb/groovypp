package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

@Typed
public class Issue140Test extends GroovyShellTestCase {
  void testBinopAssign () {
        shell.evaluate """
@Typed class Test {
    static f() {
        Reference count = [0]
        def c = { count += 1 }
        c()
        assert count == 1
    }
}
Test.f()
        """
    }

  void testWrong () {
        shouldNotCompile """
@Typed class Test {
    static f() {
        Reference count = [0]
        def c = { count += new Date() }
        c()
    }
}
Test.f()
        """
    }
}