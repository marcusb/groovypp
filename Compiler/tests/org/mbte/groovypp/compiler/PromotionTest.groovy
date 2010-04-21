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

package org.mbte.groovypp.compiler


public class PromotionTest extends GroovyShellTestCase {
  void testPromo1() {
    shell.evaluate """
    @Typed
    def u () {
      def C = '1'.toCharacter()
      def B = new Byte("1")
      assert (C + B instanceof Integer)
    }
    u ()
    """
  }

  void testPromo2() {
    shell.evaluate """
    @Typed
    def u () {
      def C = '1'.toCharacter()
      def BD = new BigDecimal("1.0")
      assert (C - BD instanceof BigDecimal)
    }
    u ()
    """
  }


  void testPromo3() {
    shell.evaluate """
    @Typed
    def u () {
      def C = '1'.toCharacter()
      def B = new Byte("1")
      def BD = new BigDecimal("1.0")

      assert (C + B instanceof Integer)
      assert (C - BD instanceof BigDecimal)
    }
    u ()
    """
  }
}