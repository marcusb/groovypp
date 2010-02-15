package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue120Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed package test

@Trait class Tr {
        String prop
        boolean getterCalled, setterCalled

        String getProp () {
            getterCalled = true
            this.prop
        }

        void setProp (String prop) {
            setterCalled = true
            this.prop = prop
        }
}

class C implements Tr {}

def c = new C()
c.prop = "HZ"
assert "HZ" == c.prop
assert c.setterCalled
assert c.getterCalled
        """
    }
}