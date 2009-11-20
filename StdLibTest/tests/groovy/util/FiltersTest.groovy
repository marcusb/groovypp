package groovy.util

public class FiltersTest extends GroovyShellTestCase {
  void testSimple() {
    assertTrue shell.evaluate("""
        @Typed
        class C extends GroovyTestCase {
          def test() {
            assertEquals([0,3], (0..5).filter{it % 3 == 0})
            assertEquals(8, [1,2,3,5,8,13].find{it == 8})
            assertNull([1,2,3,5,8,13].find{it == 9})
            assertEquals([2,8], [1,2,3,5,8,13].findAll{it % 2 == 0})
            assertTrue(((Integer[])[23,10]).any{it.toString().contains("1")})
            true
          }
        }
        new C().test()
        """)
  }

  void testInner() {
    shell.evaluate("""
        @Typed
        class C extends GroovyTestCase {
          class Job {
            List<String> children = [""]
          }
          void test() {
            def job = new Job()
            assertFalse(job.children.any {it.toLowerCase().size() > 0})
          }
        }
        new C().test()

    "" // dummy return
        """)
  }
}