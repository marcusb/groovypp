package groovy.util

import groovy.util.concurrent.BindLater

@Typed abstract class MessageChannel<T> {
    abstract void post (T message)

    static <M extends ReplyRequired, R> void sendAndContinue(MessageChannel<M> channel, M message, MessageChannel<R> replyTo) {
        message.replyTo = replyTo
        channel.post(message)
    }

    static <M extends ReplyRequired, R> Object sendAndWait(MessageChannel<M> channel, M message) {
        def binder = new BindLater()
        channel.sendAndContinue(message) { reply ->
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

    static class ReplyRequired {
        MessageChannel replyTo
    }
}