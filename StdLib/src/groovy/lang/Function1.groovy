package groovy.lang

@Trait
abstract class Function1<T,R> {
    abstract R call (T param)

    public <R1> Function1<T,R1> andThen (Function1<R,R1> g) {
        { T arg -> g.call(call(arg)) }
    }

    public <T1> Function1<T1,R> composeWith (Function1<T1,T> g) {
        { T arg -> call(g.call(arg)) }
    }

    R getAt (T arg) {
        call(arg)
    }
}