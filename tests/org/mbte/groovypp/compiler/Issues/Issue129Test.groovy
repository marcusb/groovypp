package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue129Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed class Test {
    static f() {
        def sb = new StringBuffer("")
        sb.append("x").append("y")
        sb.toString()
    }
}
assert "xy" == Test.f()
        """
    }
}