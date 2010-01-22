package org.mbte.groovypp.compiler.Issues

public class Issue53Test extends GroovyShellTestCase {
    void testProtectedSuperMethodCall () {
        shell.evaluate """
            @Typed package p
            class C extends GroovyTestCase {
              protected void setUp() {
                super.setUp()
              }
            }
            new C().setUp()
        """
    }

    void testProtectedSuperPropertyAccess () {
        shell.evaluate """
            @Typed package p
            class C extends GroovyTestCase {
              protected void foo() {
                String s = super.testClassName
              }
            }
            new C().foo()
        """
    }
}