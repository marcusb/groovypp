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

package groovy.channels

import java.util.concurrent.Executor

import groovy.util.concurrent.FQueue
import groovy.util.concurrent.CallLater

/**
 * Channel, which asynchronously schedule incoming messages for processing.
 * No more than one message processed at any given moment
 */
@Typed abstract class ExecutingChannel<M> extends QueuedChannel<M> implements Runnable {
    Executor executor
    boolean  runFair

    void run() {
        runFair ? runFair () : runNonfair ()
    }

    protected final void signalPost(FQueue<M> oldQueue, FQueue<M> newQueue) {
        if (oldQueue !== busyEmptyQueue && newQueue.size() == 1)
            executor.execute this
    }

    protected final void runFair () {
        for (;;) {
            def q = queue
            def removed = q.removeFirst()
            if (q.size() == 1) {
                if (queue.compareAndSet(q, busyEmptyQueue)) {
                    onMessage removed.first
                    if (!queue.compareAndSet(busyEmptyQueue, FQueue.emptyQueue)) {
                        executor.execute this
                    }
                    return
                }
            }
            else {
                if (queue.compareAndSet(q, removed.second)) {
                    onMessage removed.first
                    executor.execute this
                    return
                }
            }
        }
    }

    protected final void runNonfair () {
        for (;;) {
            def q = queue
            if (queue.compareAndSet(q, busyEmptyQueue)) {
                for(m in q) {
                    onMessage m
                }
                if(!queue.compareAndSet(busyEmptyQueue, FQueue.emptyQueue)) {
                    executor.execute this
                }
                break
            }
        }
    }

    protected void onMessage(M message) {
        if(message instanceof ExecuteCommand) {
            ((ExecuteCommand)message).run ()
        }
    }


    final <S> ExecuteCommand<S> schedule (ExecuteCommand<S> command) {
        post(command)
        command
    }

    abstract static class ExecuteCommand<S> extends CallLater<S> {}
}