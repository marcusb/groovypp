package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue119Test extends GroovyShellTestCase {
    void testSpreadList () {
        shell.evaluate """
@Typed
package test
class C {
  C(int i, int j) {}
}

def l = [0, 1]
new C(*l)
        """
    }
}