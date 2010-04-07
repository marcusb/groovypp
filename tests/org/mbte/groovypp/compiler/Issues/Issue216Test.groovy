package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue216Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed package p

class Test {
    public static String foo = "bar"
    static main(args) {
        assert Test.@foo == "bar"
    }
}
        """
    }
}