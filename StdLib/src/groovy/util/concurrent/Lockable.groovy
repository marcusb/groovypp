package groovy.util.concurrent

import java.util.concurrent.locks.ReentrantLock

@Trait
abstract class Lockable {
    final ReentrantLock lock = [:]

    public <R> R withLock (Function0<R> op) {
        lock.lock ()
        try {
            op.call()
        }
        finally {
            lock.unlock()
        }
    }
}
