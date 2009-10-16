package groovy

import static groovy.CompileTestSupport.shouldNotCompile
class GenericsTest extends GroovyShellTestCase {

  void testSimpleParameterization() {
    shouldNotCompile """
      @Typed
      public class Generic<T> {
          T bar() {return null}
          public void foo() {
              bar()
          }
      }
      new Generic().foo(null)
    """
  }
}