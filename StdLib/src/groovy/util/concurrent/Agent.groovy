package groovy.util.concurrent

@Typed
public class Agent<T>  {
    private volatile T ref

    private volatile FQueue<Function1<T,T>> queue = FQueue.emptyQueue

    Agent () {}

    Agent (T ref) { this.@ref = ref }
    
    final T get () { ref }

    T apply (Function1<T,T> mutation) {
        for (;;) {
            def q = queue
            def p = q.addLast(mutation)
            if (queue.compareAndSet(q, p)) {
                if (p.size == 1) {
                    CallLaterExecutors.currentExecutor.callLater {
                        for (;;) {
                            def qq = queue
                            if (!qq.size) {
                                break
                            }

                            def pp = qq.removeFirst ()
                            if (queue.compareAndSet(qq, pp.second)) {
                                def m = pp.first
                                try {
                                    ref = m(ref)
                                }
                                catch (Throwable t) {
                                    t.printStackTrace(System.err)
                                }
                            }
                        }
                    }
                }
                break
            }
        }
    }
}
