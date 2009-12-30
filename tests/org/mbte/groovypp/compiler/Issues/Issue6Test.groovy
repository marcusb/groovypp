package org.mbte.groovypp.compiler.Issues

public class Issue6Test extends GroovyShellTestCase {
    void test1 () {
        shell.evaluate """
        @Typed def foo () {
           Reference r = [1]
           int t = r
           assert t == 1
        }
        foo()
        """
    }
}