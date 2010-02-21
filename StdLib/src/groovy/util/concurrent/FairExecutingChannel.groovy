package groovy.util.concurrent

import java.util.concurrent.Executor

@Typed abstract static class FairExecutingChannel<M> extends SchedulingChannel<M>  {
    Executor executor

    FairExecutingChannel() {
        this.executor = CallLaterExecutors.getCurrentExecutor()
    }

    FairExecutingChannel(Executor executor) {
        this.executor = executor
    }

    void schedule () {
        executor.execute this
    }

    abstract void onMessage(M message)
}
