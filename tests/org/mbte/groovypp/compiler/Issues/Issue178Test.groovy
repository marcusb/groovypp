package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue178Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Immutable
@Typed
final class Punter {
    String first, last
    int third
}
assert "123" == new Punter(first:123).first
        """
    }
}
