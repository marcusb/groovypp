package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue54Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
            @Typed
            class Test {
                static main(args) {
                    println "From groovy++"
                }
            }
        """
    }
}