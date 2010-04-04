package groovy.util.concurrent

@Typed abstract class NonfairExecutingChannel<M> extends ExecutingChannel<M>  {
    void run () {
        for (;;) {
            def q = queue
            if (queue.compareAndSet(q, busyEmptyQueue)) {
                for(m in q) {
                    onMessage m
                }
                if(!queue.compareAndSet(busyEmptyQueue, FQueue.emptyQueue)) {
                    schedule ()
                }
                break
            }
        }
    }
}
