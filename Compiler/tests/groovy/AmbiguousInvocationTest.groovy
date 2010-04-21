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

class AmbiguousInvocationTest extends GroovyShellTestCase {

  void testAmbiguousInvocationWithFloats() {
    def res = shell.evaluate("""
      @Typed
      public class DummyMethodsGroovy {
          public String foo(String a, float b, float c) {
              return "f";
          }

          public String foo(String a, int b, int c) {
              return "i";
          }
      }

      @Typed
      def u(List res) {
        DummyMethodsGroovy dummy1 = new DummyMethodsGroovy();
        res << dummy1.foo("bar", 1.0f, 2.0f)
        res << dummy1.foo("bar", (float) 1, (float) 2)
        res << dummy1.foo("bar", (Float) 1, (Float) 2)
        res << dummy1.foo("bar", 1, 2)
        res << dummy1.foo("bar", (int) 1, (int) 2)
        res << dummy1.foo("bar", (Integer) 1, (Integer) 2)
        return res;
      }

      u([])
    """)
    assertEquals(["f", "f", "f", "i", "i", "i"], res)
  }
}