package groovy.lang

@Typed abstract class AbstractStruct implements Cloneable, Externalizable {
    static class Builder<T extends AbstractStruct>  {
        protected T obj

        protected Builder(T obj) {
            this.obj = obj
        }

        final T build () {
            def r = obj
            obj = null
            r
        }

        String toString() { obj.toString() }

        int hasCode () { obj.hashCode() }

        boolean equals (Object other) { obj.equals(other) }
    }

    def clone () {
        super.clone ()
    }

    String toString() {
        def sb = new StringBuilder()
        sb.append(this.class.simpleName.replace('\$','.'))
        sb.append("{")
        toString(sb)
        sb.append("}")
    }

    void toString(StringBuilder sb) {}

    abstract static class ApplyOp<B extends Builder> implements Delegating<B> {
        abstract void call ()
    }
}
