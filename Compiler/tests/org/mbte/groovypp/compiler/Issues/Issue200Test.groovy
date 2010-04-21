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

public class Issue200Test extends GroovyShellTestCase {
    void test1() {
          shell.evaluate """
@Typed package p

class Team {
    List players = []
    def getAt(int position){
      return players[position]
    }
    def putAt(int position, player){
      players[position] = player
    }
}

class Player{
    String name
    String toString() { name }
}

Team t = new Team()
assert (t[0] = new Player(name:'Roshan')).toString() == 'Roshan'
"""
    }

    void test2() {
          shell.evaluate """
@Typed package p

class Test {
    def foo(){
        def list = []
        list[0] = 1
    }
}

assert 1 == new Test().foo ()
"""
    }

}