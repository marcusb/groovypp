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

class PlusEqualsTest extends GroovyShellTestCase {

  void testIntegerPlusEquals() {
    shell.evaluate """
          @Typed
          def u() {
            def x = 1
            def y = 2
            x += y

            assert x == 3

            y += 10

            assert y == 12
          }

          u()
        """
  }

  void testCharacterPlusEquals() {
    shell.evaluate """

          @Typed
          def u() {
            Character x = 1
            Character y = 2
            x += y

            assert x == 3

            y += 10

            assert y == 12
          }

          u()
        """
  }

  void testNumberPlusEquals() {
    shell.evaluate """

          @Typed
          def u() {
            def x = 1.2
            def y = 2
            x += y

            assert x == 3.2

            y += 10.1

            assert y == 12.1
          }

          u()
        """
  }

  void testStringPlusEquals() {
    shell.evaluate """

          @Typed
          def u() {
            def x = "bbc"
            def y = 2
            x += y

            assert x == "bbc2"

            def foo = "nice cheese"
            foo += " gromit"

            assert foo == "nice cheese gromit"
          }

          u()
        """
  }

  void testSortedSetPlusEquals() {
    shell.evaluate """

          @Typed
          def u() {
            def sortedSet = new TreeSet()
            sortedSet += 'abc'
            assert (sortedSet instanceof SortedSet)
            sortedSet += ['def', 'ghi']
            assert (sortedSet instanceof SortedSet)
            assert sortedSet.size() == 3
          }

          u()
        """
  }
}
