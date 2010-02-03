package org.mbte.groovypp.compiler.Issues

@Typed
class Issue95Test extends GroovyShellTestCase {

    void testMe()
    {
        shell.evaluate """
@Typed
class Test {
    static main(args) {
        def records = '''mrhaki\tGroovy
hubert\tJava'''

        records.splitEachLine('\t') { List items ->
            println (items[0] + " likes " + items[1])
        }
    }
}
"""    }
}

