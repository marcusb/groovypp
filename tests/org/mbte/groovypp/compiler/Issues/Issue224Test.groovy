package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue224Test extends GroovyShellTestCase {
    void testCollection () {
        shell.evaluate """
@Typed package p

Collection coll = null
for(i in coll) {}
"""
    }

    void testArray () {
        shell.evaluate """
@Typed package p

int[] arr = null
for(i in arr) {}
"""
    }
}