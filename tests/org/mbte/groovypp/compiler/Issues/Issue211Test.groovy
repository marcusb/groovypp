package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldCompile

@Typed
public class Issue211Test extends GroovyShellTestCase {
    void testMe () {
        shouldCompile """
@Typed package p
class Book{
    private String title
    void setTitle(String title){}
}

Book book = new Book()
book.title = "Foo"
        """
    }

  void testBinop () {
        shell.evaluate """
@Typed package p
class C{
    public int i = 0
    void setI(int p) { i = p + 1 }
}

def c = new C()
c.i = 2
assert c.i == 3
c.i += 3
assert c.i == 7
        """
    }

  void testPrefixPostfix () {
        shell.evaluate """
@Typed package p
class C{
    public int i = 0
    void setI(int p) { i = p + 1 }
}

def c = new C()
++c.i
assert c.i == 2
c.i++
assert c.i == 4
        """
    }
}