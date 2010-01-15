package org.mbte.groovypp.compiler.Issues

public class Issue47Test extends GroovyShellTestCase {
    void testMapSort () {
        shell.evaluate """
            @Typed package p

            def map = new HashMap<String,Integer> ()
            map.sort { a, b -> a.value - b.value }
        """
    }
}