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
 * @version $Revision: 13550 $
 */
class CompareTypesTest extends GroovyShellTestCase {
  void testCompareByteToInt() {
    shell.evaluate("""
        @Typed
        def u() {
          Byte a = 12
          Integer b = 10

          assert a instanceof Byte
          assert b instanceof Integer

          assert a > b
        }
        u();
      """
    )
  }

  void testCompareByteToDouble() {
    shell.evaluate("""
        @Typed
        def u() {
          Byte a = 12
          Double b = 10

          assert a instanceof Byte
          assert b instanceof Double

          assert a > b
        }
        u();
      """
    )

  }

  void testCompareLongToDouble() {
    shell.evaluate("""
        @Typed
        def u() {
          Long a = 12
          Double b = 10

          assert a instanceof Long
          assert b instanceof Double

          assert a > b
        }
        u();
      """
    )
  }

  void testCompareLongToByte() {
    shell.evaluate("""
        @Typed
        def u() {
          Long a = 12
          Byte b = 10

          assert a instanceof Long
          assert b instanceof Byte

          assert a > b

        }
        u();
      """
    )

  }

  void testCompareIntegerToByte() {
    shell.evaluate("""
        @Typed
        def u() {
          Integer a = 12
          Byte b = 10

          assert a instanceof Integer
          assert b instanceof Byte

          assert a > b

        }
        u();
      """
    )

  }

  void testCompareCharToLong() {
    shell.evaluate("""
        @Typed
        def u() {
          def a = Integer.MAX_VALUE
          def b = ((long) a)+1
          a=(char) a

          assert a < b
        }
        u();
      """
    )
  }

  void testCompareCharToInteger() {
    shell.evaluate("""
        @Typed
        def u() {
          Character a = Integer.MAX_VALUE
          Integer b = a-1

          assert a instanceof Character
          assert b instanceof Integer

          assert a > b
        }
        u();
      """
    )
  }
} 


