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

public class InnerTest extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed
class A {
   static def field = 10
   void main (a) {
     new C ().r ()
   }

   class C {
      def r () {
        4.times { it ->
          3.times {
            new B(it).u (it)
          }
        }
      }
   }

   class B {
     def s
     B (s) { this.s = s}
     def u (i) { println i }
   }}

   static class D { def u () { field = field + 12 } } """
    }
}