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
public class Issue146Test extends GroovyShellTestCase {
    void testCloneCallOnArray () {
        shell.evaluate """
            @Typed class Test{ 
                static void main(args) {
                    String[] strs = ["a"] as String[]
                    assert strs.clone() != null 
                }
            }
        """
    }

    void testCloneCallOnArrayTrhoughAnEnum () {
        // enum's values() calls clone on an array again
        shell.evaluate """
            @Typed package test
            class Test{ 
                static main(args) {
                    assert EnumSet.allOf(MyEnum.class) != null
                }
            }
            enum MyEnum { A, B, C }
        """
    }
}