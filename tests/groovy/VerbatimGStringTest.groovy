package groovy

class VerbatimGStringTest extends GroovyShellTestCase {

    void testWithOneVariable() {
        String script =
          "@Typed\n" +
          "def u() {\n" +
          "def name = \"Bob\"\n" +
          "def template = \"\"\"\n" +
          "hello \${name} how are you?\n" +
          "\"\"\"\n\n" +

          "assert template instanceof GString\n" +        
          "assert template.getValueCount() == 1\n" +        
          "assert template.getValue(0) == \"Bob\"\n" +        
          "assert template.toString().trim() == \"hello Bob how are you?\"\n" +        

          "}\n\n" +
          "u();";
        shell.evaluate(script);
    }
    
    void testWithVariableAtEnd() {

      String script =
        "@Typed\n" +
        "def u() {\n" +
        "def name = \"Bob\"\n" +
        "def template = \"\"\"\n" +
        "hello \${name}\n" +
        "\"\"\"\n\n" +

        "assert template.toString().trim() == \"hello Bob\"\n" +
        "}\n" +
        "u()";
      shell.evaluate(script);
    }
    
    void testWithVariableAtBeginning() {

      String script =
        "@Typed\n" +
        "def u() {\n" +
        "def name = \"Bob\"\n" +
        "def template = \"\"\"\n" +
        "\${name} hey,\n" +
        "hello\n" +
        "\"\"\"\n\n" +

        "assert template.toString().trim().replaceAll( \"(\\\\r\\\\n?)|\\n\", \"\\n\" ) == \"Bob hey,\\nhello\"\n" +        

        "}\n" +
        "u()";
      shell.evaluate(script);
    }

    void testWithJustVariable() {
      "@Typed\n" +
      "def u() {\n" +
      "def name = \"Bob\"\n" +
      "def template = \"\"\"\n" +
      "\${name}\n" +
      "\"\"\"\n\n" +

      "assert template.toString().trim() == \"Bob\"\n" +

      "}\n" +
      "u()"
      ;
    }
}
