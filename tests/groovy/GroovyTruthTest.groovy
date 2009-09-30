package groovy

class GroovyTruthTest extends GroovyShellTestCase {

    void testTruth() {
      def res = shell.evaluate("""
        @Compile
        def addBool(List v, Object b) {
          if (b)
            v << true;
          else
            v << false;
        }

        @Compile
        def u(List v) {
          addBool(v, null);
          addBool(v, Boolean.TRUE);
          addBool(v, true);
          addBool(v, Boolean.FALSE);

          addBool(v, false);
          addBool(v, "");
          addBool(v, "bla");
          addBool(v, "true");
          addBool(v, "TRUE");
          addBool(v, "false");
          addBool(v, '');
          addBool(v, 'bla');

          addBool(v, new StringBuffer('bla'));
          addBool(v, new StringBuffer());
          addBool(v, Collections.EMPTY_LIST);
          addBool(v, []);
          addBool(v, [1]);
          addBool(v, [].toArray());
          addBool(v, [:]);

          addBool(v, [bla: 'some value']);
          addBool(v, 1234);
          addBool(v, 0);
          addBool(v, 0.3f);
          addBool(v, new Double(3.0f));
          addBool(v, 0.0f);
          addBool(v, new Character((char) 1));
          addBool(v, new Character((char) 0));
          addBool(v, [:]);

          return v;
        }
        u([]);
      """
      )

      assertEquals (
              [false, true, true, false, false, false,
              true, true, true, true, false, true, true,
              false, false, false, true, false, false,
              true, true, false, true, true, false,
              true, false, false], res); 
    }

    void testIteratorTruth() {
      def res = shell.evaluate("""
        @Compile
        def addBool(List v, Object b) {
          if (b)
            v << true;
          else
            v << false;
        }

        @Compile
        def u(List v) {
          addBool(v, [].iterator())
          addBool(v, [1].iterator())
        }
        u([]);
      """
      )
      assertEquals ([false, true], res);
    }

    void testEnumerationTruth() {
        def res = shell.evaluate("""
          @Compile
          def addBool(List v, Object b) {
            if (b)
              v << true;
            else
              v << false;
          }


          @Compile
          def u(List res) {
            def v = new Vector()
            addBool(res, v.elements())
            v.add(new Object())
            addBool(res, v.elements())
            return res;
          }
          u([]);
        """
        )
     assertEquals ([false, true], res); 
    }
}