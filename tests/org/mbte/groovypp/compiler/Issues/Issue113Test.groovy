package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue113Test extends GroovyShellTestCase {
    void testEachWithIndex () {
        shell.evaluate """
@Typed
package se.better.groovypp.test
[].map { it }
        """
    }
}