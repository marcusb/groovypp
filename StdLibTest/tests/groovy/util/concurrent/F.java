package groovy.util.concurrent;

import java.util.concurrent.FutureTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Callable;

public abstract class F<T,R> {
    public abstract R apply (T arg);

    public FutureTask<R> future (T arg) { return future(arg, null, null); }

    public FutureTask<R> future (T arg, Executor executor) { return future(arg, executor, null); }

    public FutureTask<R> future (final T arg, Executor executor, final F<FutureTask<R>,Object> continuation) {
        final FutureTask<R> futureTask = new FutureTask<R> ( new Callable<R> () {
                public R call() throws Exception { return apply(arg); }
            }) {
                protected void done() { if (continuation != null) continuation.apply(this); }
            };

        if (executor != null) executor.execute(futureTask);
        return futureTask;
    }
}
