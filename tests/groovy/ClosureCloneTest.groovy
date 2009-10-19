package groovy

/** 
 * @author <a href="mailto:jstrachan@protique.com">James Strachan</a>
 * @version $Revision: 4996 $
 */
class ClosureCloneTest extends GroovyShellTestCase {

    void testCloneOfClosure() {
      shell.evaluate("""
          @Typed
          def u() {
            def factor = 2
            def closure = {int it -> it * factor }

            def value = closure(5)
            assert value == 10

            // now lets clone the closure
            def c2 = (Closure)closure.clone()
            assert c2 != null

            value = c2(6)
            assert value == 12
          }
          u();
        """
      )
    }
}
