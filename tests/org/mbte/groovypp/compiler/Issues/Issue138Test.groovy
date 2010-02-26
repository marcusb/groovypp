package org.mbte.groovypp.compiler.Issues

public class Issue138Test extends GroovyShellTestCase {
    void testMe() {
        shell.evaluate """
@Typed class Test{
    static main(args) {
       def a = new Test()
       assert "Object" == a.method("", 1, 2)
    }
    def method(Object... args) { "Object" }
    def method(Integer... args) { "Integer" }
}
"""
    }
}