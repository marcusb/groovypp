package groovy

class OptionalReturnTest extends GroovyShellTestCase {

	def y
	
    void testSingleExpression() {
        shell.evaluate  """
          @Typed
          def foo() {
              'fooReturn'
          }
          
          @Typed
          def u() {
            def value = foo()
            assert value == 'fooReturn'
          }

          u()
        """
    }

    void testLastExpressionIsSimple() {
        shell.evaluate  """
          @Typed
          def bar() {
              def x = 'barReturn'
              x
          }

          @Typed
          def u() {
            def value = bar()
            assert value == 'barReturn'
          }

          u()
        """

    }

    void testLastExpressionIsBooleanExpression() {
         shell.evaluate  """

          @Typed
          boolean foo2() {
              def x = 'cheese'
              x == 'cheese'
          }

          @Typed
          boolean foo3() {
              def x = 'cheese'
              x == 'edam'
          }

          @Typed(debug=true)
          def u() {
              def value = foo2()
              assert value.class.name == 'boolean'
              assert value
              value = foo3()
              assert value.class.name == 'boolean'
              assert value == false
          }

          u()
        """
    }

    void testLastExpressionIsAssignment() {
        shell.evaluate  """
          @Typed
          class A {
            def y = "asd";
            def foo() {
              y = 'assignFieldReturn'
            }
          }

          @Typed
          def assign() {
              def x = 'assignReturn'
          }

          @Typed
          def u() {
            def value = assign()
            assert value == 'assignReturn'
            value = new A().foo()
            assert value == 'assignFieldReturn'
          }

          u()
        """
    }

    void testLastExpressionIsMethodCall() {
       shell.evaluate  """

          @Typed
          def bar() {
            foo()
          }

          @Typed
          def foo() {
            "fooReturn"
          }

          @Typed
          def u() {
            def value = bar()
            assert value == 'fooReturn'
          }

          u()
        """
    }

    void testEmptyExpression() {
      shell.evaluate  """
          @Typed
          def nullReturn() {
          }

          @Typed
          def u() {
            def value = nullReturn()
            assert value == null
          }

          u()
      """
    }

    //  now this is not a compile time error in jsr-03
    void testVoidMethod() {
        // Not sure if this _should_ compile in @Typed methods
        shell.evaluate  """
          @Typed
          def foo() {
            "fooReturn"
          }

          @Typed
          void voidMethod() {
              foo()
          }

          @Typed
          def u() {
            def value = voidMethod()
            assert value == null
          }

          u()
        """
    }

    void testNonAssignmentLastExpressions() {
        shell.evaluate  """

          @Typed
          def lastIsAssert() {
              assert 1 == 1
          }

          @Typed
          def u() {
            def value = lastIsAssert()
            assert value == null
          }

          u()
        """
    }
    
}
