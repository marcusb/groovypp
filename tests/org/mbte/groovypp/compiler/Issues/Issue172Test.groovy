package org.mbte.groovypp.compiler.Issues

public class Issue172Test extends GroovyShellTestCase {
    void testMe() {
      shell.evaluate """
      @Typed package p
      class MyThread implements Runnable, Cloneable {
        void run() {}
      }
      Cloneable[] ts = [new MyThread()] as Runnable[]
"""
    }

}