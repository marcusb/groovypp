package groovy

class TypesafeMethodTest extends GroovyShellTestCase {

    void testTypesafeMethod() {
      shell.evaluate  """
          @Typed
          Integer someMethod(Integer i) {
              return i + 1
          }

          @Typed
          def u() {
             def y = someMethod(1)
             assert y == 2
          }

          u()
        """
    }
}
