package groovy.lang

@Trait abstract class DelegatingFunction3<D,T1,T2,T3,R> implements Function3<T1,T2,T3,R>, Delegating<D> {
    R call(D delegate, T1 arg1, T2 arg2, T3 arg3) {
        this.delegate = delegate
        call (arg1, arg2, arg3)
    }
}