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

import static groovy.CompileTestSupport.shouldNotCompile

public class Issue132Test extends GroovyShellTestCase {
    void testIllegalCycleInference() {
          shouldNotCompile("""
  @Typed class Test {
      static main(args) {
          def a = 1
          def b = 2
          def c

          for (i in a..b) {
              c = i
          }
      }
  }
  """,
  "IIlegal inference inside the loop. Consider making the variable's type explicit.")
    }

  void testWeirdShouldNotHappen() {
        shell.evaluate """
@Typed
int xx() {
 def i = 0
 for (a in 2..100) {
   for (b in 2..100) {
     i++
     break;
   }
   break;
 }
}
xx()
"""
    }
}