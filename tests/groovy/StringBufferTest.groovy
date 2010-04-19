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

class StringBufferTest extends GroovyShellTestCase {
    void testSize() {
      shell.evaluate  """

        @Typed
        def u() {
          def x = new StringBuffer()
          assert x.size() == x.length()
          x = new StringBuffer('some text')
          assert x.size() == x.length()
        }

        u()
      """

    }

  /*
    void testPutAt(){
        shell.evaluate  """

          @Typed
          def u() {
            def buf = new StringBuffer('0123')

            buf[1..2] = 'xx'
            assert '0xx3' == buf.toString()
            buf = new StringBuffer('0123')
            buf[1..2] = 99
            assert '0993' == buf.toString()
            buf = new StringBuffer('0123')
            buf[0..<0] = 'xx'
            assert 'xx0123' == buf.toString(), 'border case left'
            buf = new StringBuffer('0123')
            buf[4..4] = 'xx'
            println buf.toString()
            assert '0123xx' == buf.toString(), 'border case right'
            // more weird Ranges already tested in ListTest
          }

          u()
        """

    }
    */
}