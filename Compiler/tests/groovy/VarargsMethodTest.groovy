/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

// todo: spread          
//assert varargsOnlyMethod( *['',''] ) == 2

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

println "\$args -> \${args == null ? -1 : args.length}"

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
//               assert varargsLastMethod('', *['',''] ) == 2

               // todo: GROOVY-1026
               assert varargsLastMethod('',null) == -1
               assert varargsLastMethod('',null, null) ==2
            }

            u()
          """
    );
  }

}
