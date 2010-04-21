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

  void testJustSetter () {
    shell.evaluate("""
@Typed package p
class Test {
    public void setFoo(String s) {    }
}

def c = new Test()
c.foo = 'x'
""")
  }
}