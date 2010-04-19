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

public class InnerClassTest extends GroovyShellTestCase {
    void testInner () {
        shell.evaluate """
@Typed class A {
   List res
   A () {
        res = []
        for( int i in 0..<10) {
          res << new B (i)
        }
   }

   String toString () { res.toString () }

   class B {
       C u

       B (int x) { u = new C (res)}

       class C {
            int n

            C (List r) { n = res.size() }

            String toString () { (res.size () + n).toString () }
       }

       String toString () { u.toString () }
   }
}

println new A ()
        """
    }

    void testInnerInStatic () {
        shell.evaluate """
@Typed class A {
   List res
   A () {
        res = []
        for( int i in 0..<10) {
          new B (res, i)
        }
   }

   String toString () { res.toString () }

   static class B {
       C u
       List res

       B (List res, int x) {
            this.res = res
            u = new C (res)
            res << this
       }

       class C {
            int n

            C (List r) { n = res.size() }

            String toString () { (res.size () + n).toString () }
       }

       String toString () { u.toString () }
   }
}

println new A ()
        """
    }
}