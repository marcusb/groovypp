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

package org.mbte.groovypp.compiler

public class StaticMethodTest extends GroovyShellTestCase {
  void testQualifiedStaticMethod() {
    def res = shell.evaluate("""
@Typed
def u (int d) {
  [0 + d, 30 + d, 60 + d, 90 + d].collect { int rr ->
    (int)(Math.sin((rr-d)*Math.PI/180d)*100+0.5d)
  }
}

u (3)
""")
    assertEquals([0, 50, 87, 100], res)
  }

  void testStatic() {
    def res = shell.evaluate("""
@Typed
class A {
  def u = 10

  def uu () {
      [0,1,2].collect {
        u
      }
  }
}

new A().uu ()
""")
    assertEquals([10, 10, 10], res)
  }
}