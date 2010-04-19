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

class IfElseTest extends GroovyShellTestCase {

    void testIf_NoElse() {
      shell.evaluate  """
          @Typed
          def u() {
            def x = false

            if ( true ) {
                x = true
            }

            assert x == true
          }

          u()
      """
    }

    void testIf_WithElse_MatchIf() {
      shell.evaluate  """

          @Typed
          def u() {
            def x = false
            def y = false

            if ( true ) {
                x = true
            } else {
                y = true
            }

            assert x == true
            assert y == false
          }

          u()
        """
    }

    void testIf_WithElse_MatchElse() {
      shell.evaluate  """

          @Typed
          def u() {
            def x = false
            def y = false

            if ( false ) {
                x = true
            } else {
                y = true
            }

            assert false == x
            assert true == y

          }

          u()
      """
    }

    void testIf_WithElseIf_MatchIf() {
      shell.evaluate  """

          @Typed
          def u() {
            def x = false
            def y = false

            if ( true ) {
                x = true
            } else if ( false ) {
                y = true
            }

            assert x == true
            assert y == false

          }

          u()
        """
    }

    void testIf_WithElseIf_MatchElseIf() {
      shell.evaluate  """

          @Typed
          def u() {
            def x = false
            def y = false

            if ( false ) {
                x = true
            } else if ( true ) {
                y = true
            }

            assert false == x
            assert true == y

          }

          u()
      """
    }

    void testIf_WithElseIf_WithElse_MatchIf() {
      shell.evaluate  """

          @Typed
          def u() {
            def x = false
            def y = false
            def z = false

            if ( true ) {
                x = true
            } else if ( false ) {
                y = true
            } else {
                z = true
            }

            assert x == true
            assert y == false
            assert false == z

          }

          u()
        """
    }

    void testIf_WithElseIf_WithElse_MatchElseIf() {
      shell.evaluate  """

          @Typed
          def u() {
            def x = false
            def y = false
            def z = false

            if ( false ) {
                x = true
            } else if ( true ) {
                y = true
            } else {
                z = true
            }

            assert false == x
            assert true == y
            assert false == z

          }

          u()
        """
    }

    void testIf_WithElseIf_WithElse_MatchElse() {
      shell.evaluate  """

          @Typed
          def u() {
            def x = false
            def y = false
            def z = false

            if ( false ) {
                x = true
            } else if ( false ) {
                y = true
            } else {
                z = true
            }

            assert false == x
            assert y == false
            assert true == z
          }

          u()
        """
    }
}
