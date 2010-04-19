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
