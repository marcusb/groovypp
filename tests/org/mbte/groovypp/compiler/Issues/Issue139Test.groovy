package org.mbte.groovypp.compiler.Issues

public class Issue139Test extends GroovyShellTestCase {
    void testVoidMethodHavingMapExpressionAtEnd() {
        shell.evaluate """
            @Typed class TestMap139{
                static main(args) {
                    foo()
                }
                static void foo () {
                    ["One" : [1]]
                }
            }
        """
    }

    void testVoidMethodHavingListExpressionAtEnd() {
        shell.evaluate """
            @Typed class TestList139{
                static main(args) {
                    foo()
                }
                static void foo () {
                    ["One"]
                }
            }
        """
    }
}