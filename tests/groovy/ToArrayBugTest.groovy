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

class ToArrayBugTest extends GroovyShellTestCase {

  void testToArrayBug() {
    shell.evaluate """

          @Typed
          protected Object [] getArray() {
              def list = [1, 2, 3, 4]
              def array = list.toArray()
              assert array != null
              return array
          }

          @Typed
          protected def callArrayMethod(Object [] array) {
              def list = Arrays.asList(array)
              assert list.size() == 4
              assert list == [1, 2, 3, 4]
          }


          @Typed
          def u() {
            def array = getArray()
            callArrayMethod(array)
          }

          u()
        """

  }


}
