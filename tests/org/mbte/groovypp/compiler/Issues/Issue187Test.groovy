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
import static groovy.CompileTestSupport.shouldNotCompile

public class Issue187Test extends GroovyShellTestCase {
  void testStaticInner() {
        shell.evaluate """
@Typed
class ConstructorTest {
	static void main(String[] args) {
        def v = 3
		def a = new A(2){{this.par = v}}
        assert a.par == 3
	}

	static class A {
	   int par	
       A(int par=0) {this.par=par}
	}
}        """
    }

    void testNonStaticInner() {
          shouldNotCompile ("""
  @Typed
  class ConstructorTest {
      static void main(String[] args) {
          new B(){}
      }

      class B {
      }
  }        """, "Can not instantiate non-static inner class ConstructorTest\$B in static context")
      }
}

