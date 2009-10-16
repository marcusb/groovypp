package groovy

import static groovy.CompileTestSupport.shouldNotCompile
class GenericsTest extends GroovyShellTestCase {

  void testSimpleParameterization() {
    shouldNotCompile """
      @Typed
      public class Generic<T> {
          public void foo(List<String> l) {
              l.get(0)
          }
      }
      new Generic().foo(null)
    """
  }
}