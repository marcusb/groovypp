package org.mbte.groovypp.compiler

import groovy.CompileTestSupport


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
            new X<String> ().add ("10")
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
          box.set(1)
          assert box.get().intValue() == 1

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
          assert v.length () == 4
          assert v == "abcd"
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

   void testWildcardParameter() {
     shell.evaluate """
        @Typed
        class Pair<X, Y> {}

        @Typed
        def foo (Pair<?,?> param) {
          return "foo called";
        }

        @Typed
        def u () {
          assert "foo called" == foo(new Pair<Integer, String>());
        }

        u()
      """
    }

    void testCast() {
      shell.evaluate """

        @Typed
        def foo (Object param) {
          List a = (List)param;
          assert ["a", "b"] == a;

          List<Object> b = (List<Object>)param;
          assert ["a", "b"] == b;

          List<String> c = (List<String>)param;
          assert ["a", "b"] == c;  
         }

         @Typed
         def u () {
           def par = new ArrayList<String>()
           par << "a";
           par << "b";
           foo(par);
         }

         u()
       """
     }

      void testGenericsArray() {
        shell.evaluate """
          @Typed
          def u() {
            List<String>[] listArr = new List<String>[3]
            Object[] objArr = listArr
            objArr[0] = new ArrayList<Date>()
          }
          u()
         """
       }

        void testRawType() {
          shell.evaluate """
            @Typed
            def u() {
              assert (new ArrayList<String>()).class == ArrayList
            }
            u()
           """
         }

  void testBoundsOfAGenericType() {
    shell.evaluate """
      @Typed
      class Box<A> {
        A u;
        A v;
        public <U extends A, V extends A> void foo(U u, V v) {
          this.u = u;
          this.v = v;
        }

      }

      @Typed
      def u() {
        def box = new Box<Number>()
        box.foo(10, 15L);
        assert Integer == box.u.class;
        assert Long == box.v.class;
      }
      u()
     """
   }

  void testSuperGenerics() {
    CompileTestSupport.shouldCompile """
      @Typed
      class Box<A extends Box<A>> {

      }

      @Typed
      def u() {
        def box = new Box<Box<Number>>()
      }
   """
 }
}