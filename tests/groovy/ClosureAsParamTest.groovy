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
