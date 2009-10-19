package groovy

/** 
 * Tests Closures in Groovy
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision: 4996 $
 */
class ClosureAsParamTest extends GroovyShellTestCase {

    void testSimpleBlockCall() {
      shell.evaluate("""
          @Typed
          def assertClosure(Closure block) {
              assert block != null
              block.call("hello!")
          }

          @Typed
          def u() {
            assertClosure({owner-> println(owner) })
          }
          u();
        """
      )
    }
}
