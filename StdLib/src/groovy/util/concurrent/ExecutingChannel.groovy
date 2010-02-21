package groovy.util.concurrent

import java.util.concurrent.Executor

@Typed abstract static class ExecutingChannel<M> extends SchedulingChannel<M>  {
    Executor executor

    ExecutingChannel() {
        this.executor = CallLaterExecutors.getCurrentExecutor()
    }

    ExecutingChannel(Executor executor) {
        this.executor = executor
    }

    void schedule () {
        executor.execute this
    }

    abstract void onMessage(M message)
}
