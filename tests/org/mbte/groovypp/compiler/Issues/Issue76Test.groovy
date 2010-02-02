package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue76Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate ("""
@Typed
class Test {
    static main(args) {
        def switchClosure = { ->
          switch ( 0 ) {
              case 0 : 10 ; break
               default : 20 ; break
          }
        }
        assert  10 == switchClosure ()
    }
}
        """)
    }
}