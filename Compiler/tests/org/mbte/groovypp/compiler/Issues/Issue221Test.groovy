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

import org.codehaus.groovy.control.MultipleCompilationErrorsException

@Typed
public class Issue221Test extends GroovyShellTestCase {
    void testMe () {
        def s = shell
        def res = shouldFail(MultipleCompilationErrorsException) {
            s.evaluate  """
    @Typed package p

    def classLoader = new GroovyClassLoader ()
    classLoader.parseClass '''
        @Grab(group="roshan", module="dawrani", version="0.0.1")
        class Foo {}
    '''
            """
        }
        assertTrue(res.contains("General error during conversion: Error grabbing Grapes -- [unresolved dependency: roshan#dawrani;0.0.1: not found]"))
    }
}