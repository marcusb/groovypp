package org.mbte.groovypp.compiler

public class PrefixPostfixTest extends GroovyShellTestCase {
  void testPostfixVar() {
    ["char", "int", "Integer", "byte", "Byte",
            "short", "Short", "long", "Long", "float", "Float", "double", "Double",
            "Character"].each {

      println it

      def res = shell.evaluate("""
          @Typed(debug=true)
          def u ($it val, List res) {
            while(val) {
              res.add(val--)
            }
            res
          }

            u (($it)5,[])
            """
      )

      println res
      assertEquals([5, 4, 3, 2, 1], res.collect {jt -> (int) jt })
    }
  }

  void testPostfixProp() {
    ["", "static"].each {st ->
      ["int", "Integer", "byte", "Byte",
              "short", "Short", "long", "Long", "float", "Float", "double", "Double",
              "char", "Character"].each {

        println it

        def res = shell.evaluate("""
              class U {
                public $st $it prop
              }

              @Typed
              def u (U u, List res) {
                while(u.prop) {
                  res.add(u.prop--)
                }
                res
              }

                u (new U(prop:($it)5),[])
                """
        )

        println res
        assertEquals([5, 4, 3, 2, 1], res.collect {jt -> (int) jt })
      }
    }
  }

  void testPrefixProp() {
    ["", "static"].each {st ->
      ["int", "Integer", "byte", "Byte",
              "short", "Short", "long", "Long", "float", "Float", "double", "Double",
              "char", "Character"].each {

        println it

        def res = shell.evaluate("""
            import org.mbte.groovygrid.compile.*
              class U {
                public $st $it prop
              }

              @Typed
              def u (U u, List res) {
                while(u.prop) {
                  res.add(--u.prop)
                }
                res
              }

                u (new U(prop:($it)5),[])
                """
        )

        println res
        assertEquals([4, 3, 2, 1, 0], res.collect {jt -> (int) jt })
      }
    }
  }

  void testPostfixArr() {
    ["int", "Integer", "byte", "Byte",
            "short", "Short", "long", "Long", "float", "Float", "double", "Double",
            "char", "Character"].each {

      println it

      def res = shell.evaluate("""
    @Typed
    def U ($it[] val, List res) {
      while(val[0]) {
        res.add(val[0]--)
      }
      res
    }

      U ([($it)5] as $it[],[])
      """
      )

      println res
      assertEquals([5, 4, 3, 2, 1], res.collect {jt -> (int) jt })
    }
  }

  void testPrefixVar() {
    ["int", "Integer", "byte", "Byte",
            "short", "Short", "long", "Long", "float", "Float", "double", "Double",
            "char", "Character"].each {

      println it

      def res = shell.evaluate("""
          @Typed
          def u ($it val, List res) {
            while(val) {
              res.add(--val)
            }
            res
          }

            u (($it)5,[])
            """
      )

      println res
      assertEquals([4, 3, 2, 1, 0], res.collect {jt -> (int) jt })
    }
  }

  void testPrefixArr() {
    ["int", "Integer", "byte", "Byte",
            "short", "Short", "long", "Long", "float", "Float", "double", "Double",
            "char", "Character"].each {

      println it

      def res = shell.evaluate("""
      @Typed
      def U ($it[] val, List res) {
        while(val[0]) {
          res.add(--val[0])
        }
        res
      }

        U ([($it)5] as $it[],[])
        """
      )

      println res
      assertEquals([4, 3, 2, 1, 0], res.collect {jt -> (int) jt })
    }
  }

  void testVar() {
    def types = ["int", "Integer", "byte", "Byte",
            "short", "Short", "long", "Long", "float", "Float", "double", "Double",
            "char", "Character", "BigInteger", "BigInteger"]

    types.each {jt ->
      types.each {it ->
        println "$it, $jt"

        def res = shell.evaluate("""
      @Typed
      def U (List res) {
        $it val = ($jt)5
        while(val) {
          res.add(--val)
        }
        res.collect{ kt -> (int) kt }
      }

        U ([])
        """
        )

        println res
        assertEquals([4, 3, 2, 1, 0], res)
      }
    }
  }
}