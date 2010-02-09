package groovy.lang

@Trait abstract class DelegatingFunction0<D,R> extends Function0<R> implements  Delegating<D> {
    R call(D delegate) {
        this.delegate = delegate
        call ()
    }
}