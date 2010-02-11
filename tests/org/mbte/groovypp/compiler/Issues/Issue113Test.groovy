package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue113Test extends GroovyShellTestCase {
    void testEachWithIndex () {
        shell.evaluate """
@Typed
package se.better.groovypp.test
[].map { it }
        """
    }

    void testTraitField () {
        shell.evaluate """
@Typed
package poo

@Trait class I<T> {
  T field
}

class C implements I<String> {}

def c = new C()
c.field = "aaa"
assert "aaa" == c.field 
        """
    }
}