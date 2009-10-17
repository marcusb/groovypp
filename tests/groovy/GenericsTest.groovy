package groovy

import static groovy.CompileTestSupport.shouldNotCompile
import static groovy.CompileTestSupport.shouldCompile
class GenericsTest extends GroovyShellTestCase {

  void testSimpleParameterization() {
    def res = shell.evaluate("""
      @Typed
      public class Generic<T> {
          public String foo(List<String> l) {
              l.get(0).toLowerCase()
          }
      }
      def l = new ArrayList()
      l.add("Schwiitzi Nati")
      new Generic().foo(l)
    """)
    assertEquals "schwiitzi nati", res
  }

  void testIllegalCall() {
    shouldNotCompile """
      @Typed
      public class Generic<T> {
          public String foo(List<Integer> l) {
              l.get(0).toLowerCase()
          }
      }
      new Generic().foo(null)
    """
  }

  void testSecondOrder() {
    shouldCompile """
      @Typed
      public class Generic<T> {
          public void foo(List<List<String>> ll) {
              ll.get(0).get(0).toLowerCase()
          }
      }
      def ll = new ArrayList()
      def l = new ArrayList()
      ll.add(l)
      l.add("")
      new Generic().foo(ll)
    """
  }

  void testMethodFromBase() {
    def res = shell.evaluate("""
      @Typed
      public class Generic<T> {
          public String foo(List<String> l) {
              l.iterator().next().toLowerCase()
          }
      }
      def l = new ArrayList()
      l.add("Schwiitzi Nati")
      new Generic().foo(l)
    """)
    assertEquals "schwiitzi nati", res
  }

}