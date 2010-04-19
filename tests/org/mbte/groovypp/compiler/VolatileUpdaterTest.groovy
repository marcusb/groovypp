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

public class VolatileUpdaterTest extends GroovyShellTestCase {
    void testReference () {
        def res = shell.evaluate ("""
            @Typed class A<T> {
                volatile T field

                boolean reset (T newValue) {
                   field.compareAndSet(null, newValue)
                }
            }

            new A<Integer> ().reset (10)
        """)

        assertTrue res
    }

    void testInt () {
        def res = shell.evaluate ("""
            @Typed class A {
                volatile int field

                def reset (int newValue) {
                   field.incrementAndGet()
                }
            }

            new A ().reset (1)
        """)

        assertEquals 1, res
    }

    void testLong () {
        def res = shell.evaluate ("""
            @Typed class A<T> {
                volatile long field

                def reset () {
                   field.getAndIncrement()
                   field.getAndIncrement()
                   field.getAndIncrement()
                }
            }

            new A ().reset ()
        """)

        assertEquals 2L, res
    }
}