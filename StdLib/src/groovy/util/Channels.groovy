@Typed package groovy.util

import groovy.util.concurrent.FairExecutingChannel
import java.util.concurrent.Executor
import groovy.util.concurrent.NonfairExecutingChannel

class Channels {
    /**
    * Convinience method to be create channel from closure
    */
    static <T> MessageChannel<T> channel(Executor self, NonfairExecutingChannel<T> channel) {
        channel.executor = self
        channel
    }

    /**
    * Convinience method to be create channel from closure
    */
    static <T> MessageChannel<T> fairChannel(Executor self, FairExecutingChannel<T> channel) {
        channel.executor = self
        channel
    }

    /**
    * Convinience method to be create channel from closure
    */
    static <T> MessageChannel<T> directChannel(Object ignore, MessageChannel<T> channel) {
        channel
    }

    /**
    * Convinience method to be create channel from closure
    */
    static <T> MessageChannel<T> threadChannel(Executor ignore, MessageChannel<T> channel) {
        channel
    }
}