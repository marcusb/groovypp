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

class ArrayAutoboxingTest extends GroovyShellTestCase {

  void testUnwantedAutoboxingWhenInvokingMethods() {
    def res = shell.evaluate("""
          @Typed
          def getClassName(Object o) {
             return o.class.name
          }

          @Typed
          def u(List res) {
            res << getClassName(new int[2*2])
            res << getClassName(new long[2*2])
            res << getClassName(new short[2*2])
            res << getClassName(new boolean[2*2])
            res << getClassName(new char[2*2])
            res << getClassName(new double[2*2])
            res << getClassName(new float[2*2])
            return res;
          }
          u([]);
        """
    )

    assertEquals(["[I", "[J", "[S", "[Z", "[C", "[D", "[F"], res);
  }
} 