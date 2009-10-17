package groovy

import static groovy.CompileTestSupport.shouldNotCompile
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

}