package groovy.util

import groovy.util.concurrent.BindLater
import java.util.concurrent.Executor

@Typed abstract class MessageChannel<T> {

    abstract void post (T message)

    static <M extends ReplyRequiringMessage, R> void request(MessageChannel<M> channel, M message, MessageChannel<R> replyTo) {
        message.replyTo = replyTo
        channel.post(message)
    }

    static <M extends ReplyRequiringMessage, R> Object requestAndWait(MessageChannel<M> channel, M message) {
        def binder = new BindLater()
        channel.request(message) { reply ->
            binder.set(reply)
        }
        binder.get()
    }

    final MessageChannel<T> leftShift (T message) {
        post message
        this
    }

    final MessageChannel<T> addBefore(MessageChannel<T> other) {
        def that = this;
        { message ->
            other.post message
             that.post message
        }
    }

    final MessageChannel<T> addAfter(MessageChannel<T> other) {
        def that = this;
        { message ->
             that.post message
            other.post message
        }
    }

    final MessageChannel<T> filter(Function1<T,Boolean> filter) {
        def that = this;
        { message ->
            if (filter(message))
                that.post(message)
        }
    }

    final <R> MessageChannel<R> map(Function1<T,R> mapping) {
        def that = this;
        { message ->
            that.post(mapping(message))
        }
    }

    final MessageChannel<T> async(Executor executor) {
        def that = this;
        { message ->
            executor.execute {
                that.post(message)
            }
        }
    }

    static class ReplyRequiringMessage {
        MessageChannel replyTo
    }
}