package groovy.util

import groovy.util.concurrent.FList

@Typed class Multiplexor<M> extends MessageChannel<M> {
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

    final void post(M message) {
        listeners.each { channel ->
            channel.post message
        }
    }

    static Multiplexor<M> of (MessageChannel<M> ... channels) {
        new Multiplexor().subscribe(channels)
    }
}
