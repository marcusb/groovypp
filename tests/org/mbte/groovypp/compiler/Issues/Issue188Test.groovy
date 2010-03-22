package org.mbte.groovypp.compiler.Issues

public class Issue188Test extends GroovyShellTestCase {
  void testMe() {
        shell.evaluate """
@Typed package p

assert 12 in 1..100
assert 65 in (1..100).iterator()
assert !(0 in 1.10)
int[] is = [1,2,3]
assert !(10 in is)
assert 2 in is
assert  22222222 in [22222222]
"""
    }
}