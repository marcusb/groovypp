package groovy.util.concurrent

import java.util.concurrent.locks.ReentrantReadWriteLock

@Trait
abstract class ReadWriteLockable {
    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock()

    public <R> R withReadLock (Function0<R> op) {
        lock.readLock().lock ()
        try {
            op.call()
        }
        finally {
            lock.readLock().unlock()
        }
    }

    public <R> R withWriteLock (Function0<R> op) {
        lock.writeLock().lock ()
        try {
            op.call()
        }
        finally {
            lock.writeLock().unlock()
        }
    }
}
