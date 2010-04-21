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

package groovy


class StaticThisTest extends GroovyShellTestCase {

    void testThisFail() {
      shell.evaluate  """
          @Typed
          class A {
          }

          Typed
          class B extends A {
              static def staticMethod() {
                def foo = this
                assert foo != null
                assert foo.name.endsWith("B")

                def s = super
                assert s != null
                assert s.name.endsWith("A")
              }
          }

          @Typed
          def u() {
            B.staticMethod();
          }

          u()
       """
    }


    void testThisPropertyInStaticMethodShouldNotCompile() {
      CompileTestSupport.shouldNotCompile """
            @Typed
            class A {
                def prop
                static method(){
                    this.prop
                }
            }
            """
    }

    void testSuperPropertyInStaticMethodShouldNotCompile() {
        CompileTestSupport.shouldNotCompile """
            @Typed
            class A { def prop }

            @Typed
            class B extends A {
                static method(){
                    super.prop
                }
            }
            """
    }
}
