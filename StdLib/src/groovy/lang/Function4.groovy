package groovy.lang

@Trait
abstract class Function4<T1,T2,T3,T4,R> {

    abstract R call (T1 param1, T2 param2, T3 param3, T4 param4)

    public <R1> Function4<T1,T2,T3,T4,R1> andThen (Function1<R,R1> g) {
        { T1 arg1, T2 arg2, T3 arg3, T4 arg4 -> g.call(call(arg1, arg2, arg3, arg4)) }
    }

    Function3<T2,T3,T4,R> curry (T1 arg1) {
        { T2 arg2, T3 arg3, T4 arg4 -> call(arg1, arg2, arg3, arg4) }
    }

    Function3<T2,T3,T4,R> getAt (T1 arg1) {
        curry(arg1)
    }
}