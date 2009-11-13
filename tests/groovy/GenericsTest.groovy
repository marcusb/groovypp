package groovy

import static groovy.CompileTestSupport.shouldNotCompile
import static groovy.CompileTestSupport.shouldCompile
class GenericsTest extends GroovyShellTestCase {

  void testSimpleParameterization() {
    def res = shell.evaluate("""
      @Typed
      public class Test {
          public String foo(List<String> l) {
              l.get(0).toLowerCase()
          }
      }
      def l = new ArrayList()
      l.add("Schwiitzi Nati")
      new Test().foo(l)
    """)
    assertEquals "schwiitzi nati", res
  }

  void testIllegalCall() {
    shouldNotCompile """
      @Typed
      public class Test {
          public String foo(List<Integer> l) {
              l.get(0).toLowerCase()
          }
      }
      new Test().foo(null)
    """
  }

  void testIllegalAdd() {
    shouldNotCompile """
      @Typed
      public class Test {
          public String foo(List<Integer> l) {
              l.add("")
          }
      }
      new Test().foo([])
    """
  }

  void testSecondOrder() {
    shouldCompile """
      @Typed
      public class Test {
          public void foo(List<List<String>> ll) {
              ll.get(0).get(0).toLowerCase()
          }
      }
      def ll = new ArrayList()
      def l = new ArrayList()
      ll.add(l)
      l.add("")
      new Test().foo(ll)
    """
  }

  void testMethodFromBase() {
    def res = shell.evaluate("""
      @Typed
      public class Test {
          public String foo(List<String> l) {
              l.iterator().next().toLowerCase()
          }
      }
      def l = new ArrayList()
      l.add("Schwiitzi Nati")
      new Test().foo(l)
    """)
    assertEquals "schwiitzi nati", res
  }

  void testMethodFromBase1() {
    shouldCompile """
      @Typed
      class Base<T1> {
         T1 t1
         Base(T1 t1) {this.t1 = t1}
         T1 foo() {return t1}
      }
      @Typed
      class Derived<T2> extends Base<T2> {Derived(T2 t2) {super(t2)}}
      @Typed
      class User {
        def foo() {new Derived<String>("").foo().toLowerCase()}
      }
      new User().foo()
    """
  }

  void testGenericProperty() {
    def res = shell.evaluate("""
      @Typed
      public class Generic<T> {
          T t
          Generic(T t) {this.t = t}
          public String foo(Generic<String> g) {
              g.t.toLowerCase()
          }
      }
      def g = new Generic("Schwiitzi Nati")
      g.foo(g)
    """)
    assertEquals "schwiitzi nati", res
  }

  void testGenericField() {
    def res = shell.evaluate("""
      @Typed
      public class Generic<T> {
          private T t
          Generic(T t) {this.t = t}
          public String foo(Generic<String> g) {
              g.t.toLowerCase()
          }
      }
      def g = new Generic("Schwiitzi Nati")
      g.foo(g)
    """)
    assertEquals "schwiitzi nati", res
  }

  void testClosure() {
    def res = shell.evaluate("""
      @Typed
      abstract class TransformClosure<T,R> {
        abstract R transform(T element)

        static <T,R> List<R> transform(Collection<T> c, TransformClosure<T,R> f) {
          def res = new ArrayList<R>()
          for (elem in c) {
            res.add(f.transform(elem))
          }
          return res
        }
      }
      @Typed
      static def foo() {
        def l = Arrays.asList(0)
        TransformClosure.transform(l, {int i -> String.valueOf(i)}).get(0).toLowerCase()
      }
      foo()
    """)
    assertEquals "0", res
  }

  void testFlatten() {
    def res = shell.evaluate("""
    @Typed
    def testFlatten () {
      def l = [[0,1,2], [3,4]]
      l.flatten().asList().map{it.byteValue()}
    }
    testFlatten()
    """)
    assertEquals([0, 1, 2, 3, 4], res)
  }
}