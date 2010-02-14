package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue120Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed package test

@Trait class Tr {
        String prop

        String getProp () {
            "prop: " + this.prop
        }

        void setProp (String prop) {
            this.prop = prop
        }
}

class C implements Tr {}

new C().prop
        """
    }
}