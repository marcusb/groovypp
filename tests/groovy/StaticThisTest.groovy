package groovy


class StaticThisTest extends GroovyShellTestCase {

    void testThisFail() {
      shell.evaluate  """
          @Typed
          class A {
          }

          Typed
          class B extends A {
              static def staticMethod() {
                def foo = this
                assert foo != null
                assert foo.name.endsWith("B")

                def s = super
                assert s != null
                assert s.name.endsWith("A")
              }
          }

          @Typed
          def u() {
            B.staticMethod();
          }

          u()
       """
    }


    void testThisPropertyInStaticMethodShouldNotCompile() {
      CompileTestSupport.shouldNotCompile """
            @Typed
            class A {
                def prop
                static method(){
                    this.prop
                }
            }
            """
    }

    void testSuperPropertyInStaticMethodShouldNotCompile() {
        CompileTestSupport.shouldNotCompile """
            @Typed
            class A { def prop }

            @Typed
            class B extends A {
                static method(){
                    super.prop
                }
            }
            """
    }
}
