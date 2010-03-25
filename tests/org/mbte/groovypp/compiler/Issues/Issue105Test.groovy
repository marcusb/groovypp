package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldNotCompile

public class Issue105Test extends GroovyShellTestCase {
    void testMe () {
       shouldNotCompile """
           @Typed package p

           def user = null

           println user?.address?.street

           class Address {String street}
           class User {Address address}

        """
    }
}