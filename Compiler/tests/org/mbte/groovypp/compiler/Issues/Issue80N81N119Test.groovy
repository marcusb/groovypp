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
class Issue80N81N119Test extends GroovyShellTestCase {

    void testEnumIssue80Ex1() {
        shouldCompile """
            @Typed
            class Test {
                enum Child { A, B }
                Test.Child child // or, Child child
            }
            1
        """
    }

    void testEnumIssue80Ex2() {
        shouldCompile """
            @Typed
            package a
            
            enum Foo { A,B }
            enum Bar { X,Y }
            1
        """
    }

    void testEnumIssue81Ex1() {
        shouldCompile """
            @Typed
            class Test {
                enum Child{ Franz, Ferdi, Nand }
            
                static main(args) {    }
                Child child
            }
        """
    }

    void testEnumIssue81Ex2() {
        shouldCompile """
            @Typed
            class Test {
                String name
                Child child
            
                enum Child{ Franz, Ferdi, Nand }
            }
            1
        """
    }

    void testEnumIssue81Ex3() {
        shouldCompile """
            @Typed
            package a
            
            enum Foo { A,B }
            
            println "Done"
        """
    }

    void testEnumIssue119Ex1() {
        shouldCompile """
            @Typed
            package test
            
            enum Seasons {W, S}
            
            assert Seasons.W != null
            assert Seasons.S != null
        """
    }
}