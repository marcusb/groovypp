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

package org.mbte.groovypp.compiler.Issues

public class Issue6Test extends GroovyShellTestCase {
    void test1 () {
        shell.evaluate """
        @Typed def foo () {
           Reference r = [1]
           int t = r
           assert t == 1
           Reference rr = [null]
           assert rr != null
        }
        foo()
        """
    }

    void test2 () {
        shell.evaluate """
        @Typed def foo () {
           Reference r = [1]
           assert r == 1
           def t = r + 1
           assert t == 2
           def v = 1 / r
           assert v == 1
        }
        foo()
        """
    }

    void test3 () {
        shell.evaluate """
        @Typed def foo () {
           Reference r = [1]
           assert r.intValue() == 1
        }
        foo()
        """
    }
    void test4 () {
        shell.evaluate """
        @Typed class C {
           def f1 = 11
           public def f2 = 13
        }
        @Typed def foo () {
           Reference r = [new C()]
           assert r.f1 == 11
           assert r.f2 == 13
        }
        foo()
        """
    }

    void test5 () {
        shell.evaluate """
        import java.util.concurrent.atomic.AtomicReference
        @Typed def foo () {
           AtomicReference<Map<Integer, Integer>> r = [[0:1]]
           assert r[0] == 1
        }
        foo()
        """
    }

  void test6 () {
      shell.evaluate """
      import java.util.concurrent.atomic.AtomicBoolean
      @Typed void foo () {
         Reference r = [0]
         r = 1
         assert r == 1
         AtomicBoolean ab = [true]
         ab = false
         assert ab
         assert ab == false
      }
      foo()
      """
  }
}