package org.mbte.groovypp.compiler.Issues

@Typed
class Issue86Test extends GroovyShellTestCase {

    void testMe()
    {
        shell.evaluate """
@Typed
package a

class Test {
    static String bar

    static main(args) {
        def x = new Foo().with {
            bar = "baz"
            delegate
        }
        assert "baz" == x.bar
    }
}

class Foo { String bar }
        """
    }
}

