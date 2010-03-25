package org.mbte.groovypp.compiler.Issues

public class Issue191Test extends GroovyShellTestCase {
    void testImmutableWithBigDecimal() {
        shell.evaluate """
            @Typed
            @Immutable class AccountV1 {
                BigDecimal balance
                String customer
                
                void deposit(BigDecimal amount) { }
                
                static main(args) {}
            }
            assert AccountV1 != null //cause the class to load to check VerifyError
        """
    }

    void testImmutableWithBigInteger() {
        shell.evaluate """
            @Typed
            @Immutable class AccountV2 {
                BigInteger balance
                
                void deposit(BigInteger amount) {    }
                
                static main(args) {}
            }
            assert AccountV2 != null //cause the class to load to check VerifyError
        """
    }

    void testBigDecimalWithoutImmutable() {
        shell.evaluate """
            @Typed package p
            class AccountV3 {
                BigDecimal balance
                def AccountV3(Map args) {
                    balance = args.get('balance')
                }
                static main(args) {}
            }
            assert AccountV3 != null //cause the class to load to check VerifyError
        """
    }
}
