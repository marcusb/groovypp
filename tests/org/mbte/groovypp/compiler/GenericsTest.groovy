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
            new X<String> ().add (10)
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
  
    void testGenericGetAndSet() {
      shell.evaluate """
        @Typed
        class Box<T> {

            T t;

            public void set(T t) {
                this.t = t
            }

            public T get() {t}
        }

        @Typed
        def u () {
          def box = new Box<Integer>();
          assert box.intValue() == 0

          def res = box.get();
          assert res == 1;
          assert res.class == Integer
        }

        u()
      """
    }

    void testGenericMethod() {
      shell.evaluate """
        @Typed
        class Box<T> {
          public <U> U inspect(U u){
              u
          }
        }

        @Typed
        def u () {
          def v = new Box<Integer>().inspect("abcd");
          assert v.length () == 3
          assert className == "abcd"
        }

        u()
      """
    }

    void testGenericStaticMethod() {
      shell.evaluate """
        @Typed
        static < T > T foo(T t) {t}

        @Typed
        def u () {
          def a = foo("abc");
          assert a.length() == 3
          assert a == "abc"
          assert a.class == String;
        }

        u()
      """
    }
}