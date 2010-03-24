package groovy.lang

@Typed class StructTest extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
    @Typed package p

    @Struct class TupleTest<A> {
        int x, y
        A inner

        @Struct static class StringTupleTest extends TupleTest<String> {}
    }

    def t = new TupleTest.StringTupleTest ()
    println t

    def b = TupleTest.newBuilder()
    println b

    b = t.newBuilder()
    println b

    b.x = 1
    b.y = 2
    b.inner = "papa & "
    b.inner += "mama"
    println "\$b \${b.inner.toUpperCase()}"

    def o = b.build()
    println o
        """
    }

    void testNested () {
        shell.evaluate """
    @Typed package p

    import java.util.concurrent.*
    import groovy.util.concurrent.*
    import java.util.concurrent.atomic.*

    @Struct class Person {
        String userName
        String password

        PersonalData data
    }

    @Struct class PersonalData { String  email }

    AtomicReference<FHashMap<String,Person>> users = [FHashMap.emptyMap]

    def executor = Executors.newFixedThreadPool(10)

    int n = 1000
    def cdl = new CountDownLatch(n)
    for (i in 0..<n) {
        executor.{
            def person = Person.{
                userName = 'user' + i
                password = 'password'
            }

            println "created \$person"

            users.apply{ u -> u.put(person.userName, person) }

            executor.{
              def newPerson = users['user'+i].{
                    data = data.{ email = userName+"@acme.com" }
              }

              // atomic apply
              users.apply{ u -> u.put(person.userName, newPerson) }

              println "updated: \$newPerson"
              cdl.countDown ()
            }
        }
    }
    cdl.await ()
    """
    }
}
