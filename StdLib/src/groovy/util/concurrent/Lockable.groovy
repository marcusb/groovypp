package groovy.util.concurrent

import java.util.concurrent.locks.ReentrantLock

@Trait
abstract class Lockable {
    final ReentrantLock lock = new ReentrantLock()

    public <R> R withLock (Function0<R> op) {
        lock.lock ()
        try {

        }
        finally {
            lock.unlock()
        }
    }
}

