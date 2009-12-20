package groovy.util.concurrent

import java.util.concurrent.*

/**
 * Specially optimized version of FutureTask from JDK
 */
@Typed
abstract class CallLater<V> extends BindLater<V> implements RunnableFuture<V>, Callable<V> {
    public final void run() {
        if (setRunningThread()) {
            try {
                set(call())
            }
            catch (Throwable ex) {
                setException(ex)
            }
        }
    }
}
