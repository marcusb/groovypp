package groovy.lang

public class SerialTest extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed package p

class B extends A {
        long d
}

class A implements Externalizable {
        String a
        int c
}

abstract class C extends B {
        float x

        byte[] bytes
}

class D extends C {
        boolean y

        ArrayList q
}

def bos = new ByteArrayOutputStream()
def o = new ObjectOutputStream (bos)
o.writeObject(new D(a:"lala", c:239, d:932, x:0.1f, y:false, q:[3,4,5], bytes: (byte[])[0,1,2,3]))
o.flush ()
o.close ()

println bos.size ()

def i = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))
D read = i.readObject()

assert read.a == "lala"
assert read.c == 239
assert read.d == 932
assert read.x == 0.1f
assert read.y == false
assert read.q == [3,4,5]
assert read.bytes == [0,1,2,3]
        """
    }

    void testNoArgConstructor () {
        def msg = shouldFail {
            shell.evaluate """
    @Typed package p

    class B implements Externalizable {
            B(long d) {}
    }
            """
        }
        assertTrue msg.contains("Class implementing java.io.Externalizable must have public no-arg constructor")
    }

    void testNonPublicNoArgConstructor () {
        def msg = shouldFail {
            shell.evaluate """
    @Typed package p

    protected class B implements Externalizable  {
            protected B() {}
    }
            """
        }
        assertTrue msg.contains("Class implementing java.io.Externalizable must have public no-arg constructor")
    }
}
