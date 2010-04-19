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
 * @author Hallvard Tr�tteberg
 * @version $Revision: 14683 $
 */
class ClosureCurryTest extends GroovyShellTestCase {

    void testCurry() {
      shell.evaluate("""
          @Typed
          def u() {
            def clos1 = {String s1, String s2 -> s1 + s2}
            def clos2 = clos1.curry("hi")
            def value = clos2("there")
            assert value == "hithere"

            def clos3 = {String s1, String s2, String s3 -> s1 + s2 + s3}
            def clos4 = (Closure)clos3.curry('a')
            def clos5 = (Closure)clos4.curry('b')
            def clos6 = (Closure)clos4.curry('x')
            def clos7 = (Closure)clos4.curry('f', 'g')
            value = clos5('c')
            assert value == "abc"
            value = clos6('c')
            assert value == "axc"
            value = clos4('y', 'z')
            assert value == "ayz"
            value = clos7()
            assert value == "afg"

            clos3 = {String s1, String s2, String s3 -> s1 + s2 + s3}.asWritable()
            clos4 = clos3.curry('a')
            clos5 = clos4.curry('b')
            clos6 = clos4.curry('x')
            clos7 = clos4.curry('f', 'g')
            value = clos5('c')
            assert value == "abc"
            value = clos6('c')
            assert value == "axc"
            value = clos4('y', 'z')
            assert value == "ayz"
            value = clos7()
            assert value == "afg"

            clos3 = {String s1, String s2, String s3 -> s1 + s2 + s3}
            clos4 = clos3.curry('a').asWritable()
            clos5 = clos4.curry('b').asWritable()
            clos6 = clos4.curry('x').asWritable()
            clos7 = clos4.curry('f', 'g').asWritable()
            value = clos5('c')
            assert value == "abc"
            value = clos6('c')
            assert value == "axc"
            value = clos4('y', 'z')
            assert value == "ayz"
            value = clos7()
            assert value == "afg"

            clos3 = {String s1, String s2, String s3 -> s1 + s2 + s3}
            clos4 = (Closure)clos3.curry('a').clone()
            clos5 = (Closure)clos4.curry('b').clone()
            clos6 = (Closure)clos4.curry('x').clone()
            clos7 = (Closure)clos4.curry('f', 'g').clone()
            value = clos5('c')
            assert value == "abc"
            value = clos6('c')
            assert value == "axc"
            value = clos4('y', 'z')
            assert value == "ayz"
            value = clos7()
            assert value == "afg"

            clos3 = {String s1, String s2, String s3 -> s1 + s2 + s3}
            clos4 = (Closure)clos3.curry('a').asWritable().clone()
            clos5 = (Closure)clos4.curry('b').asWritable().clone()
            clos6 = (Closure)clos4.curry('x').asWritable().clone()
            clos7 = (Closure)clos4.curry('f', 'g').asWritable().clone()
            value = clos5('c')
            assert value == "abc"
            value = clos6('c')
            assert value == "axc"
            value = clos4('y', 'z')
            assert value == "ayz"
            value = clos7()
            assert value == "afg"

          }
          u();
        """
      )

    }

    void testParameterTypes() {
      shell.evaluate("""
          @Typed
          def u() {
            def cl1 = {String s1, int i -> return s1 + i }
            assert "foo5" == cl1("foo", 5)
            assert [String, int] == cl1.getParameterTypes().toList()

            def cl2 = (Closure)cl1.curry("bla")
            assert "bla4" == cl2(4)
            assert null != cl2.getParameterTypes()
            println cl2.getParameterTypes().toList()
            assert [int] == cl2.getParameterTypes().toList()
          }
          u();
        """
      )
    }
}
