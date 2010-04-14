package groovy.util.concurrent

@Typed abstract class LoopChannel extends SupervisedChannel {
    private volatile boolean stopped

    protected abstract void doLoopAction ()

    void doStartup() {
        executor.execute {
            try {
                while (!stopped)
                    doLoopAction ()
            }
            catch(Throwable t) {
                stopped = true
                crash(t)
            }
        }
    }

    void doShutdown() {
        stopped = true
    }
}
