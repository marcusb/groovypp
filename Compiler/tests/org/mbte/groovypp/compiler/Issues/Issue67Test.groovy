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


public class Issue67Test extends GroovyShellTestCase {
    void testThisStatic () {
        shell.evaluate """
        @Typed package p
        class Test {
          static Closure cl = { String s -> s }
          static foo() {
            assert cl("Test") == "Test"
          }
        }
        Test.foo()
        """
    }

    void testThisInstance () {
        shell.evaluate """
        @Typed package p
        class Test {
          Closure cl = { String s -> s }
          def foo() {
            assert cl("Test") == "Test"
          }
        }
        new Test().foo()
        """
    }

    void testInstance () {
        shell.evaluate """
        @Typed package p
        class Test {
          Closure cl = { String s -> s }
        }
        new Test().cl("Test") == "Test"
        """
    }

    void testStatic () {
        shell.evaluate """
        @Typed package p
        class Test {
          static Closure cl = { String s -> s }
        }
        Test.cl("Test") == "Test"
        """
    }

  void testFunction2 () {
        shell.evaluate """
        @Typed package p
        class Test {
          static Function2<String, Object, String> cl = { s, obj -> s }
        }
        Test.cl("Test", []) == "Test"
        """
    }

}