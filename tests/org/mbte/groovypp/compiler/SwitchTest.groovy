package org.mbte.groovypp.compiler

public class SwitchTest extends GroovyShellTestCase {
  void testMe() {
    def res = shell.evaluate("""
    @Typed(debug=true)
    def u (v, List res) {
       switch (v) {
          case String:
              res << "string"
              break;

          case null:
              res << null
              break;

          case 0:
              res << 0

          case Number:
              res << v
              break;

          default:
              res << "???"
       }
    }

    def res = []
    u(0, res)
    u(null, res)
    u('abc', res)
    u (22.0f, res)
    u (new Object(), res)
    res
    """)
    assertEquals([0, 0, null, 'string', 22.0, '???'], res)
  }
}