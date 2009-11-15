package groovy.lang

import java.util.concurrent.FutureTask

@Trait
abstract class Function1<T,R> {

    abstract R apply (T param)

    public <R1> Function1<T,R1> andThen (Function1<R,R1> g) {
        { arg -> g.apply(apply(arg)) }
    }

    public <T1> Function1<T1,R> composeWith (Function1<T1,T> g) {
        { arg -> apply(g.apply(arg)) }
    }

    R getAt (T arg) {
        apply(arg)
    }

    Function0<R> curry (T arg) {
        { -> apply(arg) }
    }

    FutureTask<R> future (T arg) {
        [ { -> apply(arg) } ]
    }
}