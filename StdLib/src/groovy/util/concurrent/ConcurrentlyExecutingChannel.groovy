package groovy.util.concurrent

import java.util.concurrent.Executor

@Typed abstract class ConcurrentlyExecutingChannel<M> extends ExecutingChannel<M> {
    protected final int concurrencyLevel;

    ConcurrentlyExecutingChannel(int concurrencyLevel = Integer.MAX_VALUE, Executor executor = CallLaterExecutors.currentExecutor) {
        super(executor)
        this.concurrencyLevel = concurrencyLevel
    }

    protected int getConcurrencyLevel () {
        this.concurrencyLevel
    }
}
