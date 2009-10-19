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

    void testWithFields() {
        shell.evaluate """
            @Trait class WithCoord<Own extends WithCoord> {
                int x, y

                Own moveTo (int xx, int yy) {
                   x = xx
                   y = yy
                   (Own)this
                }
            }

            @Trait class WithSize<Own extends WithCoord> {
                int h, w

                Own resize (int hh, int ww) {
                   h = hh
                   w = ww
                   (Own)this
                }
            }

            @Trait class WithRadius<Own extends WithCoord> {
                int r
            }

            class Rect implements WithCoord<Rect>, WithSize<Rect> {
            }

            class Circle implements WithCoord<Circle>, WithRadius<Circle> {
            }

            def z = new Rect ().resize(5, 5).moveTo(10,10)
            assert z.x == 10 && z.y == 10 && z.h == 5 && z.w == 5
        """
    }
}