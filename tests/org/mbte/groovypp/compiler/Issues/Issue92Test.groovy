package org.mbte.groovypp.compiler.Issues

public class Issue92Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
        @Typed package a
        def oldMap = [a: [1, 2]]
        def newMap = [:]
        oldMap.inject(newMap) { Map map, Map.Entry entry -> map << entry }
        assert newMap == oldMap
        """
    }
}