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

class WhileLoopTest extends GroovyShellTestCase {

  void testVerySimpleWhile() {
    def res = shell.evaluate("""
        @Typed
        def u() {
          def x = 0;
          def m = 5;
          while ( x < m ) {
              x = x + 1
          }

          return x
        }
        u();
      """
    )
    assertEquals(5, res);
  }

  void testWhileWithEmptyBody() {
    def res = shell.evaluate("""
        @Typed
        def u() {
          int x = 3
          while (--x);
          x
        }
        u();
      """
    )

    assertEquals(0, res)
  }

  void testMoreComplexWhile() {
    shell.evaluate("""
        @Typed
        def u() {
          def x = 0
          def y = 5

          while ( y > 0 ) {
              x = x + 1
              y = y - 1
          }

          assert x == 5
        }
        u();
      """
    )
  }
}
