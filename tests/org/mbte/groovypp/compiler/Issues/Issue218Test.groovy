package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue218Test extends GroovyShellTestCase {
    void testMe () {
       shell.evaluate """
@Typed package p

class MyHashMap extends HashMap {}

def mm = new MyHashMap()
mm.property = "hey"
assert "hey" == mm.property
      """
    }
}