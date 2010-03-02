package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue151Test extends GroovyShellTestCase {
  void testMe () {
        shell.evaluate """
@Typed package test

abstract class Base {
  def foo() { "Base" }
}

class Derived extends Base {
  def foo() { "Derived" }

  def goo() {
    def i = new Inner()
    assert "Base" == i.bar()
  }

  class Inner {
    def bar() {
      super.foo()
    }
  }
}

new Derived().goo()

        """
    }

  void testSuperClosureCall () {
        shell.evaluate """
@Typed package test

abstract class Base {
  Closure foo = { it-> "Base" }
}

class Derived extends Base {
  def foo(int i) { "Derived" }

  def goo() {
    def i = new Inner()
    assert "Base" == i.bar()
  }

  class Inner {
    def bar() {
      super.foo(0)
    }
  }
}

new Derived().goo()

        """
    }

  void testSuperFromClosureCall () {
        shell.evaluate """
@Typed package test

abstract class Base {
  def foo()  { "Base" }
}

class Derived extends Base {
  def foo() { "Derived" }

  def goo() {
    def c = { -> super.foo() }
    assert "Base" == c()
  }
}

new Derived().goo()

        """
    }

}