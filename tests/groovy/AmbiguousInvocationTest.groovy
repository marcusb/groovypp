package groovy


class AmbiguousInvocationTest extends GroovyShellTestCase {

    void testAmbiguousInvocationWithFloats() {
      def res = shell.evaluate ("""
      @Compile
      public class DummyMethodsGroovy {
          public String foo(String a, float b, float c) {
              return "f";
          }

          public String foo(String a, int b, int c) {
              return "i";
          }
      }

      @Compile
      def u(List res) {
        DummyMethodsGroovy dummy1 = new DummyMethodsGroovy();
        res << dummy1.foo("bar", 1.0f, 2.0f)
        res << dummy1.foo("bar", (float) 1, (float) 2)
        res << dummy1.foo("bar", (Float) 1, (Float) 2)
        res << dummy1.foo("bar", 1, 2)
        res << dummy1.foo("bar", (int) 1, (int) 2)
        res << dummy1.foo("bar", (Integer) 1, (Integer) 2)
        return res;
      }

      u([])
    """)
      assertEquals(["f", "f", "f", "i", "i", "i"], res)
    }
}