package groovy.lang

import java.util.concurrent.Callable
import java.util.concurrent.FutureTask

@Trait
abstract class Function0<R> implements Callable<R> {

    abstract R call ()

    public <R1> Function0<R1> andThen (Function1<R,R1> g) {
        { -> g.call(call()) }
    }

    FutureTask<R> future () {
        [this]
    }
}