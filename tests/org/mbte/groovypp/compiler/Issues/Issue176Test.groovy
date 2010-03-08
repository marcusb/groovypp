package org.mbte.groovypp.compiler.Issues

public class Issue176Test extends GroovyShellTestCase {
  void testMe() {
        shell.evaluate """
            @Typed package test

            def x = [[1], 2]
            x.flatten().each {
              println it
            }
        """
    }
}