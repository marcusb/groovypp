package groovy

import static groovy.CompileTestSupport.shouldNotCompile

class TryCatchTest extends GroovyShellTestCase {

    def exceptionCalled
    def finallyCalled
	
    void testTryCatch() {
      shell.evaluate("""
        @Compile
        def u() {
          boolean catchVisited = false;
          boolean finallyVisited = false;

          try {
              assert false;
          }
          catch (AssertionError e) {
            catchVisited = true;
          }
          finally {
            finallyVisited = true;
          }

          assert catchVisited
          assert finallyVisited
        }
        u();
      """
      )

     }

    void testStandaloneTryBlockShouldNotCompile() {
        shouldNotCompile """
          @Compile
          def u() {
            try {
                assert true
            }
          }
          u()
        """
    }

     void testTryFinally() {
         shell.evaluate("""
           @Compile
           def u() {
             boolean touched = false;
             try {
             }
             finally {
                 touched = true;
             }
             assert touched;
           }
           u();
         """
         )
     }

     void testWorkingMethod() {
       shell.evaluate("""
         void workingMethod() {
            assert true , "Should never fail"
         }

         @Compile
         def u() {
           boolean catchVisited = false
           boolean finallyVisited = false

           try {
               workingMethod()
           }
           catch (AssertionError e) {
               catchVisited = true
           }
           finally {
               finallyVisited = true
           }

           assert !catchVisited
           assert finallyVisited
         }
         u();
       """
       )
    }
    
    void testTryWithReturnWithPrimitiveTypes() {
      shell.evaluate("""
        @Compile
        int intTry(){
          try {
            return 1
          } finally {}
        }

        @Compile
        long longTry(){
          try {
            return 2
          } finally {}
        }

        @Compile
        byte byteTry(){
          try {
            return 3
          } finally {}
        }

        @Compile
        short shortTry(){
          try {
            return 4
          } finally {}
        }

        @Compile
        char charTry(){
          try {
            return 'c'
          } finally {}
        }

        @Compile
        float floatTry(){
          try {
            return 1.0
          } finally {}
        }

        @Compile
        double doubleTry(){
          try {
            return 2.0
          } finally {}
        }

        @Compile
        def u() {
          assert intTry() == 1
          assert longTry() == 2
          assert byteTry() == 3
          assert shortTry() == 4
          assert charTry() == "c"
          assert floatTry() == 1.0
          assert doubleTry() == 2.0
        }
        u();
      """
      )

    }

    void testTryCatchWithUntyped(){
      shell.evaluate("""
        @Compile
        def u() {
          try {
            throw new Exception();
          } catch(e) {
            assert true
            return
          }
          assert false
        }
        u();
      """
      )

    }
    
    void testTryCatchInConstructor() {
      // the super() call construction left an
      // element on the stack, causing an inconsistent
      // stack height problem for the try-catch
      // this ensures the stack is clean after the call
      assertScript """
        @Compile
        class A {
          A() {
            super()
            try{}catch(e){}
          }
        }
        assert null != new A()
      """
    }
}
