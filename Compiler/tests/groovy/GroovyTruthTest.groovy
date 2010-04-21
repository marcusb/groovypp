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

class GroovyTruthTest extends GroovyShellTestCase {

  void testTruth() {
    def res = shell.evaluate("""
        @Typed
        def addBool(List v, boolean b) {
            v << b
        }

        @Typed
        def u(List v) {
          addBool(v, null);
          addBool(v, Boolean.TRUE);
          addBool(v, true);
          addBool(v, Boolean.FALSE);

          addBool(v, false);
          addBool(v, "");
          addBool(v, "bla");
          addBool(v, "true");
          addBool(v, "TRUE");
          addBool(v, "false");
          addBool(v, '');
          addBool(v, 'bla');

          addBool(v, new StringBuffer('bla'));
          addBool(v, new StringBuffer());
          addBool(v, Collections.EMPTY_LIST);
          addBool(v, []);
          addBool(v, [1]);
          addBool(v, [].toArray());
          addBool(v, [:]);

          addBool(v, [bla: 'some value']);
          addBool(v, 1234);
          addBool(v, 0);
          addBool(v, 0.3f);
          addBool(v, new Double(3.0f));
          addBool(v, 0.0f);
          addBool(v, new Character((char) 1));
          addBool(v, new Character((char) 0));
          addBool(v, [:]);

          return v;
        }
        u([]);
      """
    )

      [false, true, true, false, false, false,
              true, true, true, true, false, true, true,
              false, false, false, true, false, false,
              true, true, false, true, true, false,
              true, false, false].eachWithIndex { it, index ->
          assertEquals ([it, index], [res[index], index])
      };

    assertEquals(
            [false, true, true, false, false, false,
                    true, true, true, true, false, true, true,
                    false, false, false, true, false, false,
                    true, true, false, true, true, false,
                    true, false, false], res);
  }

    void testInstTruth() {
      def res = shell.evaluate("""
          @Typed
          def addBool(List v, boolean b) {
              v << b
          }

          @Typed
          def u(List v) {
            addBool(v, 1234);
            addBool(v, 0);
            addBool(v, 0.3f);
            addBool(v, new Double(3.0f));
            addBool(v, 0.0f);
            addBool(v, new Character((char) 1));
            addBool(v, new Character((char) 0));

            return v;
          }
          u([]);
        """
      )

        [true, false, true, true, false, true, false].eachWithIndex { it, index ->
            assertEquals ([it, index], [res[index], index])
        };
    }

  void testIteratorTruth() {
    def res = shell.evaluate("""
        @Typed
        def addBool(List v, boolean b) {
            v << b
        }

        @Typed
        def u(List v) {
          addBool(v, [].iterator())
          addBool(v, [1].iterator())
        }
        u([]);
      """
    )
    assertEquals([false, true], res);
  }

  void testEnumerationTruth() {
    def res = shell.evaluate("""
          @Typed
          def addBool(List v, boolean b) {
              v << b
          }


          @Typed
          def u(List res) {
            def v = new Vector()
            addBool(res, v.elements())
            v.add(new Object())
            addBool(res, v.elements())
            return res;
          }
          u([]);
        """
    )
    assertEquals([false, true], res);
  }
}