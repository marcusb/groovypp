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
public class Issue222Test extends GroovyShellTestCase {
    void testMe () {
        shouldCompile """
import java.util.concurrent.Executor

@Typed
class CLP {
  static Executor getOrElse (Closure factory) {

  }

  def f(int concurrencyLevel = Integer.MAX_VALUE,
        Executor executor = CLP.getOrElse { return  null }) {

  }

}        """
    }

    void testMe2 () {
        shouldCompile """
import java.util.concurrent.Executor

@Typed
class CLP {
  static Executor getOrElse (Closure factory) {

  }

  def f(int concurrencyLevel = Integer.MAX_VALUE,
        Executor executor = {}) {

  }

}        """
    }
}