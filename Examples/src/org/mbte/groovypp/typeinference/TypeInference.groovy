package org.mbte.groovypp.typeinference

import groovy.xml.MarkupBuilder

@Compile(debug = true)
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

    @Compile(value=CompilePolicy.MIXED, debug = true)
    void testMarkupBuilder () {
        def writer = new StringWriter()
        def mb = new MarkupBuilder (writer);
        def i = 0
        mb."do" {
            a(i){
                Integer j = i
                while (!(j++ == 5)) {
                    b("b$j")
                }
            }
            c {
            }
        }

        assertEquals """<do>
  <a>0
    <b>b1</b>
    <b>b2</b>
    <b>b3</b>
    <b>b4</b>
    <b>b5</b>
  </a>
  <c />
</do>""", writer.toString()
    }
}