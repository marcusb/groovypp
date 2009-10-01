package groovy

/**
 * todo: add BreakContinueLabelWithClosureTest (when break is used to return from a Closure)

 * @author Dierk Koenig
 */
class BreakContinueLabelTest extends GroovyShellTestCase {

  void testDeclareSimpleLabel() {
    shell.evaluate("""
        @Typed
        def u() {
          label_1: assert true
          label_2:
          assert true
        }
        u();
      """
    )
  }

  void testBreakLabelInSimpleForLoop() {
    shell.evaluate("""
        @Typed
        def u() {
          label_1: for (i in [1]) {
              break label_1
              assert false
          }
        }
        u();
      """
    )
  }

  void testBreakLabelInNestedForLoop() {
    shell.evaluate("""
        @Typed
        def u() {
          label: for (i in [1]) {
              for (j in [1]){
                  break label
                  assert false, 'did not break inner loop'
              }
              assert false, 'did not break outer loop'
          }
        }
        u();
      """
    )
  }

  void testUnlabelledBreakInNestedForLoop() {
    shell.evaluate("""
        @Typed
        def u() {
          def reached = false
          for (i in [1]) {
              for (j in [1]){
                  break
                  assert false, 'did not break inner loop'
              }
              reached = true
          }
          assert reached, 'must not break outer loop'
        }
        u();
      """
    )
  }

  void testBreakLabelInSimpleWhileLoop() {
    shell.evaluate("""
        @Typed
        def u() {
          label_1: while (true) {
              break label_1
              assert false
          }
        }
        u();
      """
    )
  }

  void testBreakLabelInNestedWhileLoop() {
    shell.evaluate("""
        @Typed
        def u() {
          def count = 0
          label: while (count < 1) {
              count++
              while (true){
                  break label
                  assert false, 'did not break inner loop'
              }
              assert false, 'did not break outer loop'
          }
        }
        u();
      """
    )
  }

  void testBreakLabelInNestedMixedForAndWhileLoop() {
    shell.evaluate("""
        @Typed
        def u() {
        def count = 0
          label_1: while (count < 1) {
              count++
              for (i in [1]){
                  break label_1
                  assert false, 'did not break inner loop'
              }
              assert false, 'did not break outer loop'
          }
          label_2: for (i in [1]) {
              while (true){
                  break label_2
                  assert false, 'did not break inner loop'
              }
              assert false, 'did not break outer loop'
          }
        }
        u();
      """
    )
  }

  void testUnlabelledContinueInNestedForLoop() {
    def res = shell.evaluate("""
        @Typed
        def u() {
          String log = ''
          for (i in [1,2]) {
              log = log + i
              for (j in [3,4]){
                  if (j==3) continue
                  log = log + j
              }
          }
          return log;
        }
        u();
      """
    )
    assertEquals '1424', res
  }

  void testContinueLabelInNestedForLoop() {
    def res = shell.evaluate("""
        @Typed
        def u() {
          String log = ''
          label: for (i in [1,2]) {
              log = log + i
              for (j in [3,4]){
                  if (j==4) continue label
                  log = log + j
              }
              log = log + 'never reached'
          }
          log
        }
        u();
      """
    )
    assertEquals '1323', res
  }

  void testBreakToLastLabelSucceeds() {
    def res = shell.evaluate("""
        @Typed
        def u() {
          one:
          two:
          three:
          for (i in 1..2) {
              break three
              return false;
          }
          return true;
        }
        u();
      """
    )
    assertTrue res;
  }

  void testBreakToOtherThanLastLabelCausesSyntaxError() {
    CompileTestSupport.shouldNotCompile """
        @Typed
        def u() {
          one: two: three: while (true)\nbreak one;
        }
        u();
       """;
  }

  void testContinueToLastLabelSucceeds() {
    def res = shell.evaluate("""
        @Typed
        def u() {
          one:
          two:
          three:
          for (i in 1..2) {
              continue three
              return false
          }
          return true;
        }
        u();
      """
    )
    assertTrue res;
  }

  void testContinueToOtherThanLastLabelCausesSyntaxError() {
    CompileTestSupport.shouldNotCompile """
        @Typed
        def u() {
          one: two: three: while (true)\ncontinue two
        }
        u();
       """;
  }
}