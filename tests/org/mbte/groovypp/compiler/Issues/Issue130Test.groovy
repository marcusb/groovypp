package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue130Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed class Test {
    String prop = ""
    def foo() {
        def c  = {this.prop = "bar"}
        c()
    }
}
new Test().foo()
        """
    }
}