package org.mbte.groovypp.compiler

public class GenericsTest extends GroovyShellTestCase {
    void testMe() {
        shell.evaluate """
          @Typed
          class X<T extends GroovyObject> extends ArrayList<T> {
              def getAbc(int i) {
                get(i).getProperty ("abc")
              }
          }

          @Typed
          def u () {
            new X ().add (10)
          }

          u ()
      """
    }

    void testMe2() {
        shell.evaluate """
          class A<R,X extends Collection<R>> extends HashMap<R,X> {
            X prop
          }

          @Typed
          def u () {
            Map<String,Collection<String>> s = new A<Integer> ()
          }
        """
    }
}