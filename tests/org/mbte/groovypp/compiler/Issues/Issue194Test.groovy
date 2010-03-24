package org.mbte.groovypp.compiler.Issues

public class Issue194Test extends GroovyShellTestCase {
  void testMe() {
        shell.evaluate """
@Typed
class Test {
    static main(args) {
        foo(1, 'a')
    }
    static foo(String... params) {    }
}
"""
    }
}