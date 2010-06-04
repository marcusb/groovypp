/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

          "assert template instanceof String\n" +
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
