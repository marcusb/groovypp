package org.mbte.groovypp.compiler.Issues

public class Issue143Test extends GroovyShellTestCase {
    void testMe() {
        shell.evaluate """
  @Typed class Test{
    Closure m = { return 'method' }
    Closure c1 = {
        def m = { return 'c1' }
        def c2 = {
          m()
          return this.m()
        }
        return c2()
    }
    static main(args) {
        def a = new Test()
        assert "method" == a.foo()
    }
    def foo() {this.c1()}
  }
  """
    }

  void testUnqualified() {
      shell.evaluate """
@Typed class Test{
  Closure m = { return 'method' }
  Closure c1 = {
      def m = { return 'c1' }
      def c2 = {
        return m()
      }
      return c2()
  }
  static main(args) {
      def a = new Test()
      assert "c1" == a.foo()
  }
  def foo() {this.c1()}
}
"""
  }

}