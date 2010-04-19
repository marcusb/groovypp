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



package groovy.lang

@Typed class StructTest extends GroovyShellTestCase {
    void testMe () {
//        shell.evaluate """
//    @Typed package p
//
//    @Struct class TupleTest<A> {
//        int x, y
//        A inner
//
//        @Struct static class StringTupleTest extends TupleTest<String> {}
//    }
//
//    def t = new TupleTest.StringTupleTest ()
//    println t
//
//    def b = TupleTest.newBuilder()
//    println b
//
//    b = t.newBuilder()
//    println b
//
//    b.x = 1
//    b.y = 2
//    b.inner = "papa & "
//    b.inner += "mama"
//    println "\$b \${b.inner.toUpperCase()}"
//
//    def o = b.build()
//    println o
//        """
    }

    void testNested () {
//        shell.evaluate """
//    @Typed package p
//
//    import java.util.concurrent.*
//    import groovy.util.concurrent.*
//    import java.util.concurrent.atomic.*
//
//    @Struct class Person {
//        String userName
//        String password
//
//        PersonalData data
//    }
//
//    @Struct class PersonalData { String  email }
//
//    AtomicReference<FHashMap<String,Person>> users = [FHashMap.emptyMap]
//
//    def executor = Executors.newFixedThreadPool(10)
//
//    int n = 1000
//    def cdl = new CountDownLatch(n)
//    for (i in 0..<n) {
//        executor.{
//            def person = Person.{
//                userName = 'user' + i
//                password = 'password'
//            }
//
//            println "created \$person"
//
//            users.apply{ u -> u.put(person.userName, person) }
//
//            executor.{
//              def newPerson = users['user'+i].{
//                    data = data.{ email = userName+"@acme.com" }
//              }
//
//              // atomic apply
//              users.apply{ u -> u.put(person.userName, newPerson) }
//
//              println "updated: \$newPerson"
//              cdl.countDown ()
//            }
//        }
//    }
//    cdl.await ()
//    """
    }
}
