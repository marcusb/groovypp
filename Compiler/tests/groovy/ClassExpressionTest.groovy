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

/** 
 * Tests the use of classes as variable expressions
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision: 4996 $
 */
class ClassExpressionTest extends GroovyShellTestCase {

    void testUseOfClass() {
      shell.evaluate("""
          @Typed
          class A {
            A() {
              def x = A
              assert x != null
            }
          }

          @Typed
          def u() {
            def x = String

            assert x != null

            assert x.getName().endsWith('String')
            assert x.name.endsWith('String')

            x = Integer

            assert x != null
            assert x.name.endsWith('Integer')

            x = GroovyTestCase

            assert x != null
            assert x.name.endsWith('GroovyTestCase')

            new A()
          }
          u();
        """
      )

    }

    void testClassPsuedoProperty() {
      shell.evaluate("""
          @Typed
          def u() {
            def x = "cheese";
            assert x.class != null
            assert x.class == x.getClass();
          }
          u();
        """
      )
    }
    
    void testPrimitiveClasses() {
      shell.evaluate("""
          @Typed
          def u() {
            assert void == Void.TYPE
            assert int == Integer.TYPE
            assert byte == Byte.TYPE
            assert char == Character.TYPE
            assert double == Double.TYPE
            assert float == Float.TYPE
            assert long == Long.TYPE
            assert short == Short.TYPE
          }
          u();
        """
      )
    }
    
    void testArrayClassReference() {
      shell.evaluate("""
          @Typed
          def u() {
            def foo = int[]
            assert foo.name == "[I"
          }
          u();
        """
      )
    }
}
