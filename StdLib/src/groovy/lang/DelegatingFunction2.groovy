package groovy.lang

@Trait abstract class DelegatingFunction2<D,T1,T2,R> implements Function2<T1,T2,R>, Delegating<D> {
    R call(D delegate, T1 arg1, T2 arg2) {
        this.delegate = delegate
        call (arg1, arg2)
    }
}