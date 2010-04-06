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
@Typed(debug=true) package p
class C{
    public int i = 0
    void setI(int p) { i = i + 2*p }
}

def c = new C()
c.i = 2
println c.i
assert c.i == 4
c.i += 3
println c.i
assert c.i == 18
        """
    }
}