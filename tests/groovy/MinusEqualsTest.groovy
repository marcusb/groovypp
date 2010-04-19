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

class MinusEqualsTest extends GroovyShellTestCase {

    void testIntegerMinusEquals() {
        shell.evaluate """

          @Typed
          def u() {
            def x = 4
            def y = 2
            x -= y

            assert x == 2

            y -= 1

            assert y == 1
          }

          u()
        """
    }

    void testCharacterMinusEquals() {
        shell.evaluate """

          @Typed
          def u() {
            Character x = 4
            Character y = 2
            x -= y

            assert x == 2

            y -= 1

            assert y == 1
          }

          u()
        """
    }

    void testNumberMinusEquals() {
        shell.evaluate """

          @Typed
          def u() {
            def x = 4.2
            def y = 2
            x -= y

            assert x == 2.2

            y -= 0.1

            assert y == 1.9

          }

          u()
        """
    }

    void testStringMinusEquals() {
        def foo = "nice cheese"
        foo -= "cheese"

        assert foo == "nice "
    }


    void testSortedSetMinusEquals() {
        shell.evaluate """

          @Typed
          def u() {
            def sortedSet = new TreeSet()
            sortedSet.add('one')
            sortedSet.add('two')
            sortedSet.add('three')
            sortedSet.add('four')
            sortedSet -= 'one'
            sortedSet -= ['two', 'three']
            assert sortedSet instanceof SortedSet
            assert sortedSet.size() == 1
            assert sortedSet.contains('four')
          }

          u()
        """
    }
}
