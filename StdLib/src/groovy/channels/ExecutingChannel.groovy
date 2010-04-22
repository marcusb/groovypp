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
import java.util.concurrent.Executor

@Typed abstract class ExecutingChannel<M> extends QueuedChannel<M> implements Runnable {
    Executor executor

    protected void signalPost(FQueue<M> oldQueue, FQueue<M> newQueue) {
        if (oldQueue !== busyEmptyQueue && newQueue.size() == 1)
            schedule ()
    }

    protected schedule() {
        executor.execute this
    }
}