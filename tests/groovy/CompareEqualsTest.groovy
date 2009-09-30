package groovy

class CompareEqualsTest extends GroovyShellTestCase {
    void testEqualsOperatorIsMultimethodAware() {
      shell.evaluate("""

        @Compile
        class Xyz {
            boolean equals(Xyz other) {
                true
            }

            boolean equals(Object other) {
                null
            }

            boolean equals(String str) {
                str.equalsIgnoreCase this.class.getName()
            }
        }

        @Compile
        def u() {
          assert new Xyz() == new Xyz()
          assert new Xyz().equals(new Xyz())
          assert !(new Xyz() == 239)
        }
        u();
      """
      )

    }
}