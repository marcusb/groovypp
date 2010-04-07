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
}