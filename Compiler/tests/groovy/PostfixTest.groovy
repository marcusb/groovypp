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

class PostfixTest extends GroovyShellTestCase {

  void testIntegerPostfix() {
    shell.evaluate """

          @Typed
          def u() {
            def x = 1

            def y = x++

            assert y == 1
            assert x == 2

            assert x++ == 2
            assert x == 3
          }

          u()
        """
  }

  /*
   This is actually test with BigDecimal,
   but I leave method name to stay consistent
   with groovy tests
  */

  void testDoublePostfix() {
    shell.evaluate """

          @Typed
          def u() {
            def x = 1.2
            def y = x++

            assert y == 1.2
            assert x++ == 2.2
            assert x == 3.2
          }

          u()
        """
  }

  void testStringPostfix() {
    shell.evaluate """

          @Typed
          def u() {
             def x = "bbc"
             x++

             assert x == "bbd"

             def y = "bbc"++
             assert y == "bbc"
          }

          u()
        """
  }



  /**
   * Not sure if this actually should work in @Typed method.
   */
  void testArrayPostfix() {
    shell.evaluate """

          @Typed
          def u() {
            int[] i = [1]

            def y = i[0]++

            assert y == 1
            assert i[0]++ == 2
            assert i[0] == 3
          }

          u()
        """
  }

  void testConstantPostfix() {
    shell.evaluate """

          @Typed
          def u() {
            assert 1 == 1++
          }

          u()
        """
  }



  void testFunctionPostfix() {
    shell.evaluate """
          int valueReturned() { 0 }

          @Typed
          def u() {
            def z = (valueReturned())++
            assert z == 0
          }

          u()
        """
  }

  void testPrefixAndPostfix() {
    shell.evaluate """

          def v() {
            def u = 0

//            assert -1 == -- u --
//            assert 0 == ++ u ++
//            assert 0 == u
            assert 0 == (u++)++
            assert 2 == u
          }

          v()
        """
  }
}
