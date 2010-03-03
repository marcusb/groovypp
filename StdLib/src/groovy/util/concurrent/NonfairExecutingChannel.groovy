package groovy.util.concurrent

import java.util.concurrent.Executor

@Typed abstract class NonfairExecutingChannel<M> extends FairExecutingChannel<M>  {
    void run () {
        for (;;) {
            def q = queue
            if (queue.compareAndSet(q, busyEmptyQueue)) {
                for(m in q) {
                    if (m)
                        onMessage m
                }
                if(!queue.compareAndSet(busyEmptyQueue, FQueue.emptyQueue)) {
                    executor.execute this
                }
                break
            }
        }
    }
}
