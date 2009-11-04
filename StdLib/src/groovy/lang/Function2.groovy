package groovy.lang

@Trait
abstract class Function2<T1,T2,R> {

    abstract R apply (T1 param1, T2 param2)

    public <R1> Function2<T1,T2,R1> andThen (Function1<R,R1> g) {
        { T1 arg1, T2 arg2 -> g.apply(apply(arg1, arg2)) }
    }

    Function1<T2,R> curry (T1 arg1) {
        { T2 arg2 -> apply(arg1, arg2) }
    }

    Function1<T2,R> getAt (T1 arg1) {
        curry arg1
    }
}