package org.mbte.groovypp.typeinference

@Compile
public class TypeInference extends GroovyTestCase {

    void testInference () {
        def list = []
        list.leftShift 1
        list.leftShift 2
        assertEquals ([1,2], list)

        if (list.size() == 2) {
            list = (Number)list [0]
            list++
            assertTrue (list instanceof Integer)
        }
        else {
            list = 239G
            assertTrue list instanceof BigDecimal
        }
        list instanceof Number
    }
}