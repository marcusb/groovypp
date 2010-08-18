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



package groovy.util

public class FiltersTest extends GroovyShellTestCase {
  void testSimple() {
    assertTrue shell.evaluate("""
        @Typed
        class C extends GroovyTestCase {
          def test() {
            assertEquals([0,3], (0..5).filter{it % 3 == 0}.asList())
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