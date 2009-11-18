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