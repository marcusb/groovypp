package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue13Test extends GroovyShellTestCase {
    void testEachWithIndex () {
        shell.evaluate """
        @Typed def u () {
            (0..10).eachWithIndex { it, index ->
                println "\$index"
            }
        }
        """
    }
}