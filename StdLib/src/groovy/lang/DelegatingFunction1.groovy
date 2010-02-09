package groovy.lang

@Trait abstract class DelegatingFunction1<D,T1,R> implements Function1<T1,R>, Delegating<D> {
    R call(D delegate, T1 arg1) {
        this.delegate = delegate
        call (arg1)
    }
}