@Typed package groovy.util

import groovy.util.concurrent.FairExecutingChannel
import java.util.concurrent.Executor
import groovy.util.concurrent.NonfairExecutingChannel

class Channels {
    /**
    * Utility method to create a channel from closure.
    */
    static <T> MessageChannel<T> channel(Executor self, NonfairExecutingChannel<T> channel) {
        channel.executor = self
        channel
    }

    /**
    * Utility method to create a channel from closure.
    */
    static <T> MessageChannel<T> fairChannel(Executor self, FairExecutingChannel<T> channel) {
        channel.executor = self
        channel
    }

    /**
    * Utility method to create a channel from closure.
    */
    static <T> MessageChannel<T> directChannel(Object ignore, MessageChannel<T> channel) {
        channel
    }

    /**
    * Utility method to create a channel from closure.
    */
    static <T> MessageChannel<T> threadChannel(Executor ignore, MessageChannel<T> channel) {
        channel
    }
}