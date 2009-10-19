package groovy

class MinusEqualsTest extends GroovyShellTestCase {

    void testIntegerMinusEquals() {
        shell.evaluate """

          @Typed(debug=true)
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
