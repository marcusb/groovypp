package org.mbte.groovypp.compiler.Issues

@Typed
class Issue79Test extends GroovyShellTestCase {

    void testMe()
    {
        shell.evaluate """
        @Typed package p
        { -> assert false }
        println "bye"
        """
    }
}