package org.mbte.groovypp.compiler.Issues

@Typed
class Issue93Test extends GroovyShellTestCase {

    void testMe()
    {
        shell.evaluate """
@Typed
package a

StringBuilder sb = []
sb <<"Alex "
assert (sb + 239) == "Alex 239"
        """
    }
}

