package groovy

class ArrayParamMethodTest extends GroovyShellTestCase {

    void testMethodCall() {

      /*
      shell.evaluate("""
          @Typed
          void methodWithArrayParam(String[] args) {
              // lets turn it into a list
              def list = args.toList()
              assert list instanceof java.util.List
              list[4] = "e"

              assert list == ["a", "b", "c", null, "e"]
          }

          @Typed
          def u() {
            def array = "a b c".split(' ')
            assert array.size() == 3
            methodWithArrayParam(array)
          }
          u();
        """
      )
      */
    }
}