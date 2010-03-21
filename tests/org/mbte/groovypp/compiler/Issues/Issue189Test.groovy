package org.mbte.groovypp.compiler.Issues

public class Issue189Test extends GroovyShellTestCase {
  void testMe() {
        shell.evaluate """
@Typed package p

class A extends LinkedHashMap {
   Function1 ree = { Map.Entry e -> removeEldestEntry(e) }
}

new A().ree(null)
"""
    }
}