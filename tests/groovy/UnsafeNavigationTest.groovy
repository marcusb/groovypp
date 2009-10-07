package groovy

class UnsafeNavigationTest extends GroovyShellTestCase {

    void testUnsafePropertyNavigations() {
      CompileTestSupport.shouldNotCompile ( """
          @Typed
          def u() {
            def x = null
            def y = x.foo
          }

          u()
        """);
  
    }
}
