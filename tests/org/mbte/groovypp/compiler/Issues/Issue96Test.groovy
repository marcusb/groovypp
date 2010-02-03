package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue96Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed
class Test {
    static main(args) {
        def m = [id: 1, name: 'Roshan']
        assert m == [id: 1, name: 'Roshan']
        assert [id: 1, name: 'Roshan'] == m
    }
}
        """
    }
}