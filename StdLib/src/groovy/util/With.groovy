package groovy.util

@Typed class With {
    abstract static class WithOp<T,R> implements Delegating<T> {
        abstract R call ()
    }

    static <T,R> R with(T self, WithOp<T,R> op) {
        op.delegate = self
        op.call ()
    }
}