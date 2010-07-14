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
class Issue252Test extends GroovyShellTestCase {

    void testAssignVar () {
        shell.evaluate """
    @Typed package p

    class Foo {
        def prop
    }

    class Bar extends Foo {}

    def v = new Foo ()
    def f = false, g = false
    Foo x = f ? [] : (g ? new Foo() : v)
    assert x == v

    Foo y = !f ? [prop:'aaa'] : (g ? new Foo() : v)
    assert y.prop == 'aaa'

    def h =!f ? new Foo ( prop: v ? 'bbb' : 'ccc') : new Bar ()
    println h.class
    println h.prop
    assert h.prop == 'bbb'
        """
    }

    void testMe()
    {
        shouldCompile """
    @Typed package p

    class Foo {
     def foo() {}
    }

    List<Foo> list = []
    def x = [new Foo()]
    def xs = true ? x : list
    xs.each { it.foo() }
        """
    }
}