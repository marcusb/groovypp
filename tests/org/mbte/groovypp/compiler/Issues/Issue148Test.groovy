package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue148Test extends GroovyShellTestCase {
  void testMe () {
        shell.evaluate """
@Typed class Test{
    static main(args) {
       Helper h = new Helper()
       [1].each(h.&foo)
    }
}

class Helper {
    def foo(x) {}
}
        """
    }

  void testReceiverEval () {
        shell.evaluate """
@Typed class Test{
    static main(args) {
       [1, 2, 3].each(new Helper().&foo)
       assert 1 == Helper.instancesCreated
       assert 3 == Helper.methodCalled
    }
}

class Helper {
    static int instancesCreated, methodCalled
    Helper() {
        instancesCreated++
    }

    def foo(x) {
      methodCalled++
    }
}
        """
    }
}