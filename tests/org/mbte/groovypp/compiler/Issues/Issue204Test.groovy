package org.mbte.groovypp.compiler.Issues

public class Issue204Test extends GroovyShellTestCase {
    void test1() {
          shell.evaluate """
@Typed package pppp

class Test {
    def num = 0

    void setNum(num) {}
    def getNum() { 10 }

    def foo() {
        assert num  == 0
        num = 1
        assert this.num == 1
    }
}
new Test().foo()
"""
    }
}