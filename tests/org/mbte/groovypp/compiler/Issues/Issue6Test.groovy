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

    void test2 () {
        shell.evaluate """
        @Typed def foo () {
           Reference r = [1]
           assert r == 1
           def t = r + 1
           assert t == 2
           def v = 1 / r
           assert v == 1
        }
        foo()
        """
    }

    void test3 () {
        shell.evaluate """
        @Typed def foo () {
           Reference r = [1]
           assert r.intValue() == 1
        }
        foo()
        """
    }
}