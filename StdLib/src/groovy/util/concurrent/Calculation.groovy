package groovy.util.concurrent

import java.util.concurrent.*

@Typed
abstract class Calculation<V> extends BindLater<V> implements Future<V>, Runnable {
    V get() throws InterruptedException, ExecutionException {
        if (setRunningThread()) {
            try {
                run()
            }
            catch (Throwable ex) {
                setException(ex)
            }
        }
        super.get()
    }
}
