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
import groovy.util.concurrent.FList

@Typed abstract class ResourcePool<R> {
    Executor executor
    boolean  runFair

    private volatile Pair<FQueue<Function1<R,?>>,FList<R>> state = [FQueue.emptyQueue,null]

    abstract FList<R> initResources ()

    final void execute (Function1<R,?> action) {
        if (state.second == null) {
            initPool ()
        }
        for (;;) {
            def s = state
            if (s.second.empty) {
                // no resource available, so put action in to the queue
                if(state.compareAndSet(s, [s.first.addLast(action), FList.emptyList]))
                    break
            }
            else {
                // queue is guaranteed to be empty
                if(state.compareAndSet(s, [FQueue.emptyQueue, s.second.tail])) {
                    // schedule action
                    executor.execute {
                        scheduledAction(action,s.second.head)
                    }
                    break
                }
            }
        }
    }

    private final void scheduledAction(Function1<R,?> action, R resource) {
        action(resource)
        for (;;) {
            def s = state
            if (s.first.empty) {
                // no more actions => we return resource to the pool
                if(state.compareAndSet(s, [FQueue.emptyQueue, s.second + resource]))
                    break
            }
            else {
                def removed = s.first.removeFirst()
                if(state.compareAndSet(s, [removed.second, s.second])) {
                    if (runFair) {
                        // schedule action
                        executor.execute {
                            scheduledAction(removed.first,resource)
                        }
                    }
                    else
                        removed.first.call(resource)
                }
            }
        }
    }

    private synchronized void initPool () {
        if (state.second == null) {
            state.second = initResources ()
        }
    }
}
