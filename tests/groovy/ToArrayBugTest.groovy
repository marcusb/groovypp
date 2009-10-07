package groovy

class ToArrayBugTest extends GroovyShellTestCase {

  void testToArrayBug() {
    shell.evaluate """

          @Typed
          protected Object [] getArray() {
              def list = [1, 2, 3, 4]
              def array = list.toArray()
              assert array != null
              return array
          }

          @Typed
          protected def callArrayMethod(Object [] array) {
              def list = Arrays.asList(array)
              assert list.size() == 4
              assert list == [1, 2, 3, 4]
          }


          @Typed
          def u() {
            def array = getArray()
            callArrayMethod(array)
          }

          u()
        """

  }


}
