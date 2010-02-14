package org.mbte.groovypp.compiler.Issues

import static groovy.CompileTestSupport.shouldCompile

@Typed
class Issue80N81N119Test extends GroovyShellTestCase {

    void testEnumIssue80Ex1() {
        shouldCompile """
            @Typed
            class Test {
                enum Child { A, B }
                Test.Child child // or, Child child
            }
            1
        """
    }

    void testEnumIssue80Ex2() {
        shouldCompile """
            @Typed
            package a
            
            enum Foo { A,B }
            enum Bar { X,Y }
            1
        """
    }

    void testEnumIssue81Ex1() {
        shouldCompile """
            @Typed
            class Test {
                enum Child{ Franz, Ferdi, Nand }
            
                static main(args) {    }
                Child child
            }
        """
    }

    void testEnumIssue81Ex2() {
        shouldCompile """
            @Typed
            class Test {
                String name
                Child child
            
                enum Child{ Franz, Ferdi, Nand }
            }
            1
        """
    }

    void testEnumIssue81Ex3() {
        shouldCompile """
            @Typed
            package a
            
            enum Foo { A,B }
            
            println "Done"
        """
    }

    void testEnumIssue119Ex1() {
        shouldCompile """
            @Typed
            package test
            
            enum Seasons {W, S}
            
            assert Seasons.W != null
            assert Seasons.S != null
        """
    }
}