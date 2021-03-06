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

class ChainedAssignmentTest extends GroovyShellTestCase {


    void testCompare() {
      shell.evaluate("""
          @Typed
          def dummy(v) {
              print v
          }

          @Typed
          def u() {
            def i = 123
            def s = "hello"

            def i2
            def i1 = i2 = i;
            assert i1 == 123
            assert i2 == 123

            def s1
            dummy(s1 = s)
            assert s1 == "hello"
          }
          u();
        """
      )
    }
}
