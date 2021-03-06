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

class CastTest extends GroovyShellTestCase {

  void testCast() {
    shell.evaluate("""
        @Typed
        def methodWithShort(Short) {
          return "short"
        }

        @Typed
        def u() {
          def s = (Short) 5
          methodWithShort(s)
        }
        u();
      """
    )
  }

  void testIntCast() {
    shell.evaluate("""
        @Typed
        def u() {
          def i = (Integer) 'x'
          assert i instanceof Integer
        }
        u();
      """
    )
  }

  void testCharCompare() {
    shell.evaluate("""
        @Typed
        def u() {
          def i = (Integer) 'x'
          def c = 'x'

          assert i == c
          assert i =='x'
          assert c == 'x'
          assert i == i
          assert c == c

          assert 'x' == 'x'
          assert 'x' == c
          assert 'x' == i
        }
        u();
      """
    )

  }

  void testCharCast() {
    shell.evaluate("""
        @Typed
        def u() {
          def c = (Character) 'x'
          assert c instanceof Character
          c = (Character)10
          assert c instanceof Character
        }
        u();
      """
    )
  }

  void testPrimitiveCasting() {
    shell.evaluate("""
        @Typed
        def u() {
          def d = 1.23
          def i1 = (int)d
          def i2 = (Integer)d
          assert i1.class.name == 'int'
          assert i2.class.name == 'java.lang.Integer'

          def ch = (char) i1
          assert ch.class.name == 'char'

          def dd = (double)d
          assert dd.class.name == 'double'
        }
        u();
      """
    )
  }

  void testAsSet() {
    shell.evaluate("""
        @Typed
        def u() {
          def mySet = [2, 3, 4, 3] as SortedSet
          assert mySet instanceof SortedSet

          // identity test
          mySet = {} as SortedSet
          assert mySet.is ( mySet as SortedSet )

          mySet = [2, 3, 4, 3] as Set
          assert mySet instanceof HashSet

          // identitiy test
          mySet = {} as Set
          assert mySet.is ( mySet as Set )

          // array test
          mySet = new String[2] as Set // Array of 2 null Strings
          assert mySet instanceof Set
          assert mySet.size() == 1
          assert mySet.iterator().next() == null

          mySet = "a,b".split(",") as Set // Array of 2 different Strings
          assert mySet instanceof Set
          assert mySet.size() == 2
          assert mySet == new HashSet([ "a", "b" ])

          mySet = "a,a".split(",") as Set // Array of 2 different Strings
          assert mySet instanceof Set
          assert mySet.size() == 1
          assert mySet == new HashSet([ "a" ])
        }
        u();
      """
    )
  }

  void testCastToAbstractClass() {
    shell.evaluate("""
        @Typed
        def u() {
          def closure = { 42 }
          def myList = closure as AbstractList
          assert myList[-1] == 42
          assert myList.size() == 42
        }
        u();
      """
    )
  }

  void testListConstructor () {
      shell.evaluate """
              @Typed package p

              class A {
                List<Integer> ints

                A (List<Integer> ints) {
                   this.ints = ints
                }
              }

              assert new A(0, 1, 2, 3).ints == [0, 1, 2, 3]

              A a = [3,4,5]
              assert a.ints == [3,4,5]

              a = []
              assert a.ints == []
            """
  }

    void testSuperConstructorInMapSyntax () {
        shell.evaluate """
                @Typed package p

                class A {
                  int a, b
                  A (int a, int b) {
                      this.a = a
                      this.b = b
                  }
                }

                assert new A(0, 1).b == 1

                A a = [ 'super': [-1, 1] ]

                assert a.a == -1
                assert a.b == 1
              """
    }
}
