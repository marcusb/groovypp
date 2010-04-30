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

import groovy.util.concurrent.FQueue

/**
 * Message channel with incoming queue
 */
@Typed abstract class QueuedChannel<M> extends MessageChannel<M> {

    protected volatile FQueue<M> queue = FQueue.emptyQueue

    /**
     * Special tag saying that processing thread(reader) are processing last message in the queue.
     * This is kind of protocol between writers to QueuedChannel and reader.
     */
    protected static final FQueue busyEmptyQueue = FQueue.emptyQueue + null

    final void post(M message) {
        for (;;) {
            def oldQueue = queue
            def newQueue = (oldQueue === busyEmptyQueue ? FQueue.emptyQueue : oldQueue) + message
            if (queue.compareAndSet(oldQueue, newQueue)) {
                signalPost(oldQueue, newQueue)
                return
            }
        }
    }

    /**
     * Action (normally scheduling logic) to be taken after a message placed in to incoming queue
     */
    protected abstract void signalPost (FQueue<M> oldQueue, FQueue<M> newQueue)

    /**
     * Asynchronous message processing callback
     */
    protected abstract void onMessage(M message)
}