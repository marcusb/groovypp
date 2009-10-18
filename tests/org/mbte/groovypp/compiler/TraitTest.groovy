package org.mbte.groovypp.compiler

public class TraitTest extends GroovyShellTestCase {
    void testSimple() {
        shell.evaluate """
            @Trait class X {
                def getX () {
                   "x"
                }

                abstract def getY ()
            }

            @Trait class Y extends X {
                def getY () {
                   "y"
                }

                def getX () {
                   "xx"
                }
            }

            abstract class Z implements X, Y {
            }

            class ZZ extends Z {
            }

            def z = new ZZ ()
            println z.class
            assert z.getX () == 'xx' && z.getY () == 'y'
        """
    }
}