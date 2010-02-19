package groovy.util

import groovy.util.concurrent.FList
import groovy.util.concurrent.FQueue
import java.util.concurrent.Executor
import groovy.util.concurrent.CallLaterExecutors

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

    abstract static class ExecutingChannel<M> extends MessageChannel<M> implements Runnable {
        Executor executor

        protected volatile FQueue<M> queue = FQueue.emptyQueue

        ExecutingChannel() {
            this.executor = CallLaterExecutors.getCurrentExecutor()
        }

        ExecutingChannel(Executor executor) {
            this.executor = executor
        }

        MessageChannel<M> post(M message) {
            for (;;) {
                def q = queue
                def newQ = queue.addLast(message)
                if (queue.compareAndSet(q, newQ)) {
                    if (q.size < concurrencyLevel)
                        executor.execute this
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
                    if (q.size > concurrencyLevel)
                        executor.execute this
                    return
                }
            }
        }

        abstract void onMessage(M message)

        protected int getConcurrencyLevel () { 1 }
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
}