package groovy.util.concurrent

@Typed
public class Agent<T>  {
    private volatile T ref

    private volatile FQueue<Function1<T,T>> queue = FQueue.emptyQueue

    private volatile int count

    T get () { ref }

    T apply (Function1<T,T> mutation) {
        for (;;) {
            def q = queue
            def p = q.addLast(mutation)
            if (queue.compareAndSet(q, p)) {
                if (!count.getAndIncrement()) {
                    callLater(CallLaterExecutors.currentExecutor) {
                        int n = 0
                        for (;;) {
                            count.getAndIncrement()

                            for(;;) {
                                for (;;) {
                                    def qq = queue
                                    def pp = qq.removeFirst ()
                                    if (queue.compareAndSet(qq, pp.second)) {
                                        def m = pp.first
                                        ref = m(ref)
                                        n++
                                        break
                                    }
                                }
                                if (count.decrementAndGet() == 1)
                                    break;
                            }

                            if (!count.decrementAndGet())
                                break;
                        }
                    }
                }
                break
            }
        }
    }
}
