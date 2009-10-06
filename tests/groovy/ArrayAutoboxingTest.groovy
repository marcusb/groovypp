package groovy

class ArrayAutoboxingTest extends GroovyShellTestCase {

  void testUnwantedAutoboxingWhenInvokingMethods() {
    def res = shell.evaluate("""
          @Typed
          def getClassName(Object o) {
             return o.class.name
          }

          @Typed(debug=true)
          def u(List res) {
            res << getClassName(new int[2*2])
            res << getClassName(new long[2*2])
            res << getClassName(new short[2*2])
            res << getClassName(new boolean[2*2])
            res << getClassName(new char[2*2])
            res << getClassName(new double[2*2])
            res << getClassName(new float[2*2])
            return res;
          }
          u([]);
        """
    )

    assertEquals(["[I", "[J", "[S", "[Z", "[C", "[D", "[F"], res);
  }
} 