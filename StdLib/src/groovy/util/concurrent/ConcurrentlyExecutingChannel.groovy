package groovy.util.concurrent

import java.util.concurrent.Executor

@Typed abstract class ConcurrentlyExecutingChannel<M> extends FairExecutingChannel<M> {
    protected final int concurrencyLevel;

    ConcurrentlyExecutingChannel(int concurrencyLevel = Integer.MAX_VALUE, Executor executor = CallLaterExecutors.currentExecutor) {
        this.concurrencyLevel = concurrencyLevel
    }

    protected int getConcurrencyLevel () {
        this.concurrencyLevel
    }
}
