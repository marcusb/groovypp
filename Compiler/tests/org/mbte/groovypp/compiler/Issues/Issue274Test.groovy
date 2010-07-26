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

@Typed
public class Issue274Test extends GroovyShellTestCase {
    void testMe () {
       shell.evaluate """
        @Typed package p

def foo(String[] s){ s.length }
String bar(String[] s){ s[0] }

assert bar([]) == ''
assert foo([]) == 1
assert bar(['1234']).substring(1) == '234'
assert foo('1234') == 1
        """
    }

    void testWithCast () {
        shell.evaluate """
        @Typed package p

class Test {
    def a, b
    Test(String a, String b) {this.a = a; this.b = b}
}

def foo(Test[] s){ s }

Object[] ret
/* 1) - start */
ret = foo(['a1', 'b1'])
assert ret.class.array
assert ret.length == 1
assert ret[0].dump().contains('a=a1 b=b1')
"""
    }
}