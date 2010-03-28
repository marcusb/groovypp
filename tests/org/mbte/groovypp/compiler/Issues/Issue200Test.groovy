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