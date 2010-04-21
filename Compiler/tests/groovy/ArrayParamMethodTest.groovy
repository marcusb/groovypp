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

class ArrayParamMethodTest extends GroovyShellTestCase {

    void testMethodCall() {

      /*
      shell.evaluate("""
          @Typed
          void methodWithArrayParam(String[] args) {
              // lets turn it into a list
              def list = args.toList()
              assert list instanceof java.util.List
              list[4] = "e"

              assert list == ["a", "b", "c", null, "e"]
          }

          @Typed
          def u() {
            def array = "a b c".split(' ')
            assert array.size() == 3
            methodWithArrayParam(array)
          }
          u();
        """
      )
      */
    }
}