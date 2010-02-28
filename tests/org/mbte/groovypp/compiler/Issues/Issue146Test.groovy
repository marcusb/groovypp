package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue146Test extends GroovyShellTestCase {
    void testCloneCallOnArray () {
        shell.evaluate """
            @Typed class Test{ 
                static void main(args) {
                    String[] strs = ["a"] as String[]
                    assert strs.clone() != null 
                }
            }
        """
    }

    void testCloneCallOnArrayTrhoughAnEnum () {
        // enum's values() calls clone on an array again
        shell.evaluate """
            @Typed package test
            class Test{ 
                static main(args) {
                    assert EnumSet.allOf(MyEnum.class) != null
                }
            }
            enum MyEnum { A, B, C }
        """
    }
}