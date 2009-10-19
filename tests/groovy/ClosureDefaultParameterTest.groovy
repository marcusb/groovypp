package groovy

/** 
 * @author <a href="mailto:jstrachan@protique.com">James Strachan</a>
 * @version $Revision: 9213 $
 */
class ClosureDefaultParameterTest extends GroovyShellTestCase {

    void testClosureWithDefaultParams() {
      shell.evaluate("""
          @Typed
          def u() {
            def block = {a = 123, b = 456 -> println "value of a = \$a and b = \$b" }

            block = { Integer a = 123, String b = "abc" ->
                      println "value of a = \$a and b = \$b"; return "\$a \$b".toString() }

            assert block.call(456, "def") == "456 def"
            assert block.call() == "123 abc"
            assert block(456) == "456 abc"
            assert block(456, "def") == "456 def"
          }
          u();
        """
      )
    }
    
    void testClosureWithDefaultParamFromOuterScope() {
      shell.evaluate("""
          @Typed
          def u() {
            def y = 555
            def boo = {x = y -> x}
            assert boo() == y
            assert boo(1) == 1
          }
          u();
        """
      )
    }

}

