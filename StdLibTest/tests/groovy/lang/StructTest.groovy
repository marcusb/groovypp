package groovy.lang

@Typed class StructTest extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
    @Typed(debug=true) package p

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
}

//class StructTest<A> extends AbstractStruct {
//    private int x, y
//    private A inner
//
//    StructTest () {
//    }
//
//    StructTest (int x, int y) {
//        this.x = x
//        this.x = y
//    }
//
//    static Builder newBuilder(StructTest<A> self = null) {
//        new Builder(self)
//    }
//
//    int getX ()   { x }
//    int getY ()   { y }
//    A getInner () { inner }
//
//    String toString() { "[x:$x, y:$y, inner:$inner]" }
//
//    int hashCode () {
//        int h = 1
//        h = 31*h + x
//        h = 31*h + y
//        h = 31*h + inner?.hashCode ()
//        h
//    }
//
//    boolean equals (Object o) {
//        if (o instanceof StructTest) {
//            StructTest other = o
//            other.x == x && other.y == y && other.inner == inner
//        }
//    }
//
//    static class Builder<T extends StructTest,A> extends AbstractStruct.Builder<T> {
//        Builder (T obj = null) {
//            super(obj != null ? (T)obj.clone() : new StructTest())
//        }
//
//        int  getX ()      { obj.x     }
//        void setX (int x) { obj.x = x }
//
//        int  getY ()      { obj.y     }
//        void setY (int x) { obj.y = y }
//
//        A    getInner ()        { obj.inner }
//        void setInner (A inner) { obj.inner = inner }
//    }
//
//    static class StringTupleTest extends StructTest<String> {
//        static class Builder<T extends StringTupleTest> extends StructTest.Builder<T,String> {
//            Builder (T obj = null) {
//                super(obj != null ? (T)obj.clone() : new StringTupleTest())
//            }
//        }
//
//        static Builder newInstance() {
//            new Builder()
//        }
//
//        Builder builder () {
//            new Builder(this)
//        }
//    }
//
//    static void main (String [] args ) {
//        def b = StringTupleTest.newInstance()
//        b.x = 1
//        b.y = 2
//        b.inner = "mama"
//        println "$b ${b.inner.toUpperCase()}"
//
//        def o = b.build()
//        println o
//    }
//}
