package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue213Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed class Test {
    static main(args) {
        def d = '''Line 1

            Line 2.
        '''
        def f = File.createTempFile("Test", ".txt")
        f.deleteOnExit ()

        f.withWriter { w ->
            d.eachLine { w.println it }
        }
        def t = f.text
        println "'\$t'"
    }
}        """
    }
}