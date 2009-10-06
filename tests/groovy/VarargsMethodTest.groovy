package groovy

/** 
 * VarargsMethodTest.groovy
 *
 *   1) Test to fix the Jira issues GROOVY-1023 and GROOVY-1026.
 *   2) Test the feature that the length of arguments can be variable
 *      when invoking methods with or without parameters.
 *
 * @author Dierk Koenig
 * @author Pilho Kim
 * @author Hein Meling
 * @version $Revision: 4996 $
 */

class VarargsMethodTest extends GroovyShellTestCase {

    void testVarargsOnly() {
     shell.evaluate(
      """
         @Typed
         Integer varargsOnlyMethod(Object[] args) {
             // (1) todo: GROOVY-1023 (Java 5 feature)
             //     If this method having varargs is invoked with no parameter,
             //     then args is not null, but an array of length 0.
             // (2) todo: GROOVY-1026 (Java 5 feature)
             //     If this method having varargs is invoked with one parameter
             //     null, then args is null, and so -1 is returned here.
             if (args == null)
                   return -1
             return args.size()
         }

        @Typed
        def u() {
          assert varargsOnlyMethod('') == 1
          assert varargsOnlyMethod(1) == 1
          assert varargsOnlyMethod('','') == 2
          assert varargsOnlyMethod( ['',''] ) == 1
          assert varargsOnlyMethod( ['',''] as Object[]) == 2
          assert varargsOnlyMethod( *['',''] ) == 2

          // todo: GROOVY-1023
          assert varargsOnlyMethod() == 0

          // todo: GROOVY-1026
          assert varargsOnlyMethod(null) == -1
          assert varargsOnlyMethod(null, null) == 2 

        }

		u()
      """
      );
    }


     void testVarargsLast() {
        shell.evaluate(
          """
            @Typed
            Integer varargsLastMethod(Object first, Object[] args) {
               // (1) todo: GROOVY-1026 (Java 5 feature)
               //     If this method having varargs is invoked with two parameters
               //     1 and null, then args is null, and so -1 is returned here.
               if (args == null)
                     return -1
               return args.size()
            }

            @Typed
            def u() {
               assert varargsLastMethod('') == 0
               assert varargsLastMethod(1) == 0
               assert varargsLastMethod('','') == 1
               assert varargsLastMethod('','','') == 2
               assert varargsLastMethod('', ['',''] ) == 1
               assert varargsLastMethod('', ['',''] as Object[]) == 2
               assert varargsLastMethod('', *['',''] ) == 2

               // todo: GROOVY-1026
               assert varargsLastMethod('',null) == -1
               assert varargsLastMethod('',null, null) ==2
            }

            u()
          """
        );
     }
  
}
