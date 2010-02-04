package org.mbte.groovypp.compiler.Issues

@Typed
class Issue97Test extends GroovyShellTestCase {

    void testMe()
    {
        shell.evaluate """
@Typed
class Test {
    static main(args) {
        def map = [a:3, b:2, c:1]
        map.sort { it.value }
    }
}        """
    }
}

