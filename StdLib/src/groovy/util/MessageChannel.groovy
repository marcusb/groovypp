package groovy.util

import groovy.util.concurrent.FList
import groovy.util.concurrent.FQueue
import java.util.concurrent.Executor
import groovy.util.concurrent.CallLaterExecutors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.LockSupport
import groovy.util.concurrent.SchedulingChannel
import groovy.util.concurrent.ExecutingChannel

@Trait abstract class MessageChannel<T> {
    abstract MessageChannel<T> post (T message)

    MessageChannel<T> leftShift (T msg) {
        post msg
    }
}