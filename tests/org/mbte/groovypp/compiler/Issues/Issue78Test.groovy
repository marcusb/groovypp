package org.mbte.groovypp.compiler.Issues

public class Issue78Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
        @Typed package p
        class Test {
          static main(args) {
            def map = ['key' : null]
            map.putAt('key', 'V')
          }
        }
        """
    }
}