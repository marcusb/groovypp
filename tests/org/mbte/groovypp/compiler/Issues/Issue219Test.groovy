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

import static groovy.CompileTestSupport.shouldNotCompile

@Typed
public class Issue219Test extends GroovyShellTestCase {
    void test1 () {
        shouldNotCompile("""
@Typed package p

def m = [:]
m['key'] + ""
        """, "Cannot find method Object.plus(String)")
    }

    void test2 () {
        shouldNotCompile("""
@Typed package p

def arr = []
arr[0] + 1
        """, "Cannot find method Object.plus(int)")
    }

    void test3 () {
        shouldNotCompile("""
@Typed package p

def list = [new Date()]
list.add(1)
        """, "Cannot find method ArrayList<Date>.add(int)")
    }
}