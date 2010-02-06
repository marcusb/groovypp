package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue103Test extends GroovyShellTestCase {
    void testMe () {
        shouldFail {
            shell.evaluate """
    @Typed
    class Test {
        static main(args) {
            println [].x
        }
    }
            """
        }
    }
}