package org.mbte.groovypp.compiler.Issues

@Typed
public class Issue123Test extends GroovyShellTestCase {
    void testClassLoadingWithAMethodCalledOnAnArray () {
        shell.evaluate """
            @Typed package test
            
            class Foo {
                static main(args) {
                    Foo[] foos = [] as Foo[]
                    foos.clone()
                }
            }
            
            assert Foo != null // make sure that class loading throws no VerifyError
        """
    }
}