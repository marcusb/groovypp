package org.mbte.groovypp.compiler

public class GStringTest extends GroovyShellTestCase {

  void testCounter() {
    def res = shell.evaluate("""
class Counter {
  public int value

  Counter next () {
    value++
    this
  }

  String toString () { value.toString() }
}

@Typed
def m () {
  def counter = new Counter ()
  def gs = "counter \$counter"

  while (!(counter.value == 5)) {
    counter++
    println gs
  }
}

m ()
        """)
  }
}