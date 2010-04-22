/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package groovy.util.concurrent

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.AbstractQueuedSynchronizer
import java.util.concurrent.*

/**
 */
@Typed
class BindLater<V> extends AbstractQueuedSynchronizer implements Future<V> {
    // any of this bits mean that calculation either completed or (with S_RUNNING) about to complete
    protected static final int S_SET           = 1
    protected static final int S_EXCEPTION     = 2
    protected static final int S_CANCELLED     = 4

    protected static final int S_RUNNING       = 8

    protected static final int S_DONE = S_SET|S_EXCEPTION|S_CANCELLED

    // contains either null or running thread or result or exception
    private volatile def internalData

    private volatile FList listeners = FList.emptyList

    final boolean isCancelled() {
        def s = getState()
        (s & S_CANCELLED) && !(s & S_RUNNING)
    }

    final boolean isException() {
        def s = getState()
        (s & S_EXCEPTION) && !(s & S_RUNNING)
    }

    final boolean isSet() {
        def s = getState()
        (s & S_SET) && !(s & S_RUNNING)
    }

    final boolean isDone() {
        def s = getState()
        (s & S_DONE) && !(s & S_RUNNING)
    }

    final boolean cancel(boolean mayInterruptIfRunning) {
        for (;;) {
            def s = getState()
            if (s & S_DONE)
                return false
            if (compareAndSetState(s, S_CANCELLED|S_RUNNING)) {
                if (mayInterruptIfRunning) {
                    Thread r = internalData
                    if (r != null)
                        r.interrupt()
                }
                releaseShared(S_CANCELLED)
                done()
                return true
            }
        }
    }

    V get() throws InterruptedException, ExecutionException {
        acquireSharedInterruptibly(0)
        def s = getState()
        if (s == S_CANCELLED)
            throw new CancellationException()
        if (s == S_EXCEPTION) {
            def throwable = (Throwable) internalData
            throw new ExecutionException(throwable.message, throwable)
        }
        (V)internalData
    }

    final V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!tryAcquireSharedNanos(0, unit.toNanos(timeout)))
            throw new TimeoutException()
        def s = getState()
        if (s == S_CANCELLED)
            throw new CancellationException()
        if (s == S_EXCEPTION) {
            def throwable = (Throwable) internalData
            throw new ExecutionException(throwable.message, throwable)
        }
        (V)internalData
    }

    final BindLater<V> whenBound (Listener<V> listener) {
        for (;;) {
            def l = listeners
            if (l == null) {
                // it mean done worked already
                invokeListener(listener)
                return this
            }

            if(listeners.compareAndSet(l,l + listener))
                return this
        }
    }

    private void invokeListener (Listener<V> listener) {
        try {
            listener.onBound(this)
        }
        catch (Throwable t) {
            t.printStackTrace(System.err)
        }
    }

    protected final void done() {
        for (;;) {
            def l = listeners
            if (listeners.compareAndSet(l, null)) {
                for (el in l.reverse()) {
                    if (el instanceof BlockingQueue) {
                        ((BlockingQueue)el).put(this)
                        continue
                    }

                    if (el instanceof Listener) {
                        invokeListener((Listener)el)
                    }
                }
                return
            }
        }
    }

    public boolean set(V v) {
        for (;;) {
            def s = getState()
            if (s & S_DONE)
                return false
            if (compareAndSetState(s, S_SET|S_RUNNING)) {
                def internal = internalData
                internalData = v
                releaseShared(S_SET)
                done()
                return true
            }
        }
    }

    protected boolean setException(Throwable t) {
        for (;;) {
            def s = getState()
            if (s & S_DONE)
                return false
            if (compareAndSetState(s, S_EXCEPTION|S_RUNNING)) {
                def internal = internalData
                internalData = t
                releaseShared(S_EXCEPTION)
                done()
                return true
            }
        }
    }

    protected final int tryAcquireShared(int ignore) {
        isDone() ? 1 : -1
    }

    protected final boolean tryReleaseShared(int finalState) {
        setState(finalState)
        true
    }

    protected final boolean setRunningThread () {
        def t = Thread.currentThread ()
        for (;;) {
            def s = getState ()

            if (s) {
                return false
            }

            if (compareAndSetState(0, S_RUNNING)) {
                internalData = t
                return true
            }
        }
    }

    abstract static class Listener<V> {
        abstract void onBound (BindLater<V> data)
    }

    static class Group<V> extends BindLater<V> {
        AtomicInteger counter

        Group (int concurrency) {
            counter = [concurrency]
        }

        void attach (BindLater<V> inner) {
            inner.whenBound { bl ->
                if (!isDone()) {
                    if (bl.exception) {
                        try {
                            bl.get ()
                        }
                        catch (ExecutionException e) {
                            setException(e.cause)
                        }
                    }
                    else {
                        if (!counter.decrementAndGet())
                            set(null)
                    }
                }
            }
        }
    }
}
