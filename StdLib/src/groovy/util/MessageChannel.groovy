package groovy.util

import groovy.util.concurrent.FList
import groovy.util.concurrent.FQueue
import java.util.concurrent.Executor
import groovy.util.concurrent.CallLaterExecutors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.LockSupport

@Trait abstract class MessageChannel<T> {
    abstract MessageChannel<T> post (T message)

    MessageChannel<T> leftShift (T msg) {
        post msg
    }

    static class Multiplexor<M> implements MessageChannel<M> {
        private volatile FList<MessageChannel<M>> listeners = FList.emptyList

        Multiplexor<M> subscribe(MessageChannel<M> channel) {
            for (;;) {
                def l = listeners
                if (listeners.compareAndSet(l, l + channel))
                    return this
            }
        }

        Multiplexor<M> subscribe(MessageChannel<M> ... channels) {
            for (c  in channels) {
                subscribe(c)
            }
            this
        }

        Multiplexor<M> unsubscribe(MessageChannel<M> channel) {
            for (;;) {
                def l = listeners
                if (listeners.compareAndSet(l, l - channel))
                    return this
            }
        }

        final Multiplexor<M> post(M message) {
            listeners.each { channel ->
                channel.post message
            }
            this
        }
    }

    abstract static class SchedulingChannel<M> extends MessageChannel<M> {
        protected volatile FQueue<M> queue = FQueue.emptyQueue

        SchedulingChannel() {
        }

        MessageChannel<M> post(M message) {
            for (;;) {
                def q = queue
                def newQ = queue.addLast(message)
                if (queue.compareAndSet(q, newQ)) {
                    if (q.size() < concurrencyLevel)
                        schedule ()
                    return this
                }
            }
        }

        void run () {
            for (;;) {
                def q = queue
                def newQ = queue.removeFirst()
                if (queue.compareAndSet(q, newQ.second)) {
                    onMessage(newQ.first)
                    if (q.size() > concurrencyLevel)
                        schedule ()
                    return
                }
            }
        }

        abstract void onMessage(M message)

        abstract void schedule ()

        protected int getConcurrencyLevel () { 1 }
    }

    abstract static class ExecutingChannel<M> extends SchedulingChannel<M> implements Runnable {
        Executor executor

        ExecutingChannel() {
            this.executor = CallLaterExecutors.getCurrentExecutor()
        }

        ExecutingChannel(Executor executor) {
            this.executor = executor
        }

        void schedule () {
            executor.execute this
        }

        abstract void onMessage(M message)
    }

    abstract static class ConcurrentlyExecutingChannel<M> extends ExecutingChannel<M> {
        protected final int concurrencyLevel;

        ConcurrentlyExecutingChannel(int concurrencyLevel = Integer.MAX_VALUE, Executor executor = CallLaterExecutors.currentExecutor) {
            super(executor)
            this.concurrencyLevel = concurrencyLevel
        }

        protected int getConcurrencyLevel () {
            this.concurrencyLevel
        }
    }

    static class ChannelExecutor extends SchedulingChannel<Runnable> implements Executor {
        final int threadNumber

        private volatile FList<Thread> waitingThread = FList.emptyList

        ChannelExecutor (int threadNumber) {
            this.threadNumber = threadNumber
            def startLock = new CountDownLatch(threadNumber)
            for (i in 0..<threadNumber) {
                Thread t = [
                    run: {
                        initThread (startLock)
                        loopThread ()
                    },
                    daemon:true    
                ]
                t.start ()
            }
            startLock.await()
        }

        void onMessage(Runnable command) {
        }

        private void initThread (CountDownLatch startLock) {
            def thisThread = Thread.currentThread()
            for (;;) {
                def wt = waitingThread
                if (waitingThread.compareAndSet(wt, wt + thisThread)) {
                    startLock.countDown()
                    LockSupport.park()
                    break
                }
            }
        }

        private void loopThread () {
            def thisThread = Thread.currentThread()
            for (;;) {
                def q = queue
                if (!q.size()) {
                    def wt = waitingThread
                    if (waitingThread.compareAndSet(wt, wt + thisThread)) {
                        LockSupport.park()
                    }
                    continue
                }

                def newQ = q.removeFirst()
                if (queue.compareAndSet(q, newQ.second)) {
                    newQ.first.run ()
                }
            }
        }

        public void schedule() {
            for (;;) {
                def wt = waitingThread
                if (!wt.size())
                    break;

                if (waitingThread.compareAndSet(wt, wt.tail)) {
                    LockSupport.unpark(wt.head)
                    break;
                }
            }
        }

        public void execute(Runnable command) {
            post command
        }

        protected int getConcurrencyLevel () {
            threadNumber
        }
    }
}