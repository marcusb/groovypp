package org.mbte.groovypp.compiler

public class MixedModeTest extends GroovyShellTestCase {

  void testMe() {
    def res = shell.evaluate("""
    import groovy.xml.*

    @Typed(value=TypePolicy.MIXED)
    class A {
        void m () {
            def writer = new StringWriter()
            def mb = new MarkupBuilder (writer);
            def i = 0
            mb."do" {
     //           a(i){
                    Integer j = i
                    while (!(j++ == 5)) {
                        b("b\$j")
                    }
    //            }
    //            c {
    //            }
            }
            writer.toString ()
        }
    }

    new A ().m ()
""")
  }

}