package groovy.util

class WithTest extends GroovyShellTestCase {
    void testCompile () {
        shell.evaluate """
        @Typed package p

        class U {
            int called

            U m () { this }
        }

        assert new U ().with{
           called++;
           def t = delegate
           t.called++
           t.m()
        }.m().called == 2

        """
    }
}