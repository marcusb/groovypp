package org.mbte.groovypp.compiler.Issues

public class Issue6Test extends GroovyShellTestCase {
    void test1 () {
        shell.evaluate """
        @Typed def foo () {
           Reference r = [1]
           int t = r
           assert t == 1
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
         assert ab == false
      }
      foo()
      """
  }
}