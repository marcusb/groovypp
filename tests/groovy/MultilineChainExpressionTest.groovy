package groovy

class MultilineChainExpressionTest extends GroovyTestCase {
   void testMultiLineChain() {
     CompileTestSupport.shouldCompile """
        // the code below should be compileable
        @Typed
        def u() {
           assert (
               System
                   .out
                   .class
               ==
               PrintStream
                   .class
           )
           assert System
                  .err
                  .class == PrintStream.class
        }
       """;
   }
}