package groovy.lang

@Trait
abstract class Function3<T1,T2,T3,R> {

    abstract R apply (T1 param1, T2 param2, T3 param)

    public <R1> Function3<T1,T2,T3,R1> andThen (Function1<R,R1> g) {
        { T1 arg1, T2 arg2, T3 arg3 -> g.apply(apply(arg1, arg2, arg3)) }
    }

    Function2<T2,T3,R> curry (T1 arg1) {
        { T2 arg2, T3 arg3 -> apply(arg1, arg2, arg3) }
    }

    Function2<T2,T3,R> getAt (T1 arg1) {
        curry(arg1)
    }
}