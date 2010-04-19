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

import static groovy.CompileTestSupport.shouldNotCompile
import static groovy.CompileTestSupport.shouldCompile
import org.codehaus.groovy.ast.GenericsType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

class GenericsTest extends GroovyShellTestCase {

  void testSimpleParameterization() {
    def res = shell.evaluate("""
      @Typed
      public class Test {
          public String foo(List<String> l) {
              l.get(0).toLowerCase()
          }
      }
      def l = new ArrayList()
      l.add("Schwiitzi Nati")
      new Test().foo(l)
    """)
    assertEquals "schwiitzi nati", res
  }

  void testIllegalCall() {
    shouldNotCompile """
      @Typed
      public class Test {
          public String foo(List<Integer> l) {
              l.get(0).toLowerCase()
          }
      }
      new Test().foo(null)
    """
  }

  void testIllegalAdd() {
    shouldNotCompile """
      @Typed
      public class Test {
          public String foo(List<Integer> l) {
              l.add(new Date())
          }
      }
      new Test().foo([])
    """
  }

  void testSecondCall() {
    shell.evaluate """
      @Typed
      public class Test {
          public void foo(List<List<String>> ll) {
              assert "a" == ll.get(0).get(0).toLowerCase()
          }
      }
      def ll = [["A"]]
      new Test().foo(ll)
    """
  }

  void testMethodFromBase() {
    def res = shell.evaluate("""
      @Typed
      public class Test {
          public String foo(List<String> l) {
              l.iterator().next().toLowerCase()
          }
      }
      def l = new ArrayList()
      l.add("Schwiitzi Nati")
      new Test().foo(l)
    """)
    assertEquals "schwiitzi nati", res
  }

  void testMethodFromBase1() {
    shouldCompile """
      @Typed
      class Base<T1> {
         T1 t1
         Base(T1 t1) {this.t1 = t1}
         T1 foo() {return t1}
      }
      @Typed
      class Derived<T2> extends Base<T2> {Derived(T2 t2) {super(t2)}}
      @Typed
      class User {
        def foo() {new Derived<String>("").foo().toLowerCase()}
      }
      new User().foo()
    """
  }

  void testGenericProperty() {
    def res = shell.evaluate("""
      @Typed
      public class Generic<T> {
          T t
          Generic(T t) {this.t = t}
          public String foo(Generic<String> g) {
              g.t.toLowerCase()
          }
      }
      def g = new Generic("Schwiitzi Nati")
      g.foo(g)
    """)
    assertEquals "schwiitzi nati", res
  }

  void testGenericField() {
    def res = shell.evaluate("""
      @Typed
      public class Generic<T> {
          private T t
          Generic(T t) {this.t = t}
          public String foo(Generic<String> g) {
              g.t.toLowerCase()
          }
      }
      def g = new Generic("Schwiitzi Nati")
      g.foo(g)
    """)
    assertEquals "schwiitzi nati", res
  }

  void testClosure() {
    def res = shell.evaluate("""
      @Typed
      abstract class TransformClosure<T,R> {
        abstract R transform(T element)

        static <T,R> List<R> transform(Collection<T> c, TransformClosure<T,R> f) {
          def res = new ArrayList<R>()
          for (elem in c) {
            res.add(f.transform(elem))
          }
          return res
        }
      }
      @Typed
      static def foo() {
        def l = Arrays.asList(0)
        TransformClosure.transform(l, {int i -> String.valueOf(i)}).get(0).toLowerCase()
      }
      foo()
    """)
    assertEquals "0", res
  }

  void testInferenceInNew() {
    def res = shell.evaluate("""
    @Typed
    class Pair<T1,T2> {
      T1 first
      T2 second
      Pair(T1 t1, T2 t2) {
        this.first = t1
        this.second = t2
      }
    }

    @Typed
    def u() {
      def a = "Schwiitzi", b = ""
      new Pair(a,b).first.toLowerCase()
    }

    u()
    """)
    assertEquals "schwiitzi", res
  }

  void testWildcardInference() {
    Object res = shell.evaluate("""
    import java.util.concurrent.Callable
    import java.util.concurrent.ExecutorService
    import java.util.concurrent.Executors
    import java.util.concurrent.Future
    @Typed
    class C {
      static ArrayList<Callable<Map<String, String>>> createFragmentTasks() {
        return [[call: {["a":"b"]}]]
      }
      static def foo(ExecutorService pool) {
        def tasks = createFragmentTasks()
        return pool.invokeAll(tasks)[0].get().get("a")
      }
    }
    C.foo(Executors.newFixedThreadPool(10))
    """)
    assertEquals "b",res
  }

  void testFoldLeft() {
    Object res = shell.evaluate("""
    @Typed def foo() {
      Object[] arr = [1]
      Object[] arr2 = [arr]
      (0..0).foldLeft(arr2) {i, a -> (Object[])a[i]}
    }
    foo()
    """)
    assertEquals 1, res[0]
  }
}