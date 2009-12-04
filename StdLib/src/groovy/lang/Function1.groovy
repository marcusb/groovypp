package groovy.lang

import java.util.concurrent.FutureTask
import java.util.concurrent.Executor

@Trait
abstract class Function1<T,R> {

    abstract R call (T param)

    public <R1> Function1<T,R1> andThen (Function1<R,R1> g) {
        { arg -> g(call(arg)) }
    }

    public <T1> Function1<T1,R> composeWith (Function1<T1,T> g) {
        { arg -> call(g(arg)) }
    }

    R getAt (T arg) {
        call(arg)
    }

    Function0<R> curry (T arg) {
        { -> call(arg) }
    }

    FutureTask<R> future (T arg) {
        [ { -> call(arg) } ]
    }
}