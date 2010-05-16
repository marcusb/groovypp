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

package groovy.util.concurrent

import java.util.concurrent.Executor

@GrUnit({
    testWithFixedPool(10) {
        ResourcePool<String> cassandraPool = [
            executor: pool,
            initResources: { ["a"] }
        ]

        cassandraPool.execute { it.toUpperCase() } { assert it == 'A' }
    }
})
@Typed abstract class ResourcePool<R> {
    Executor executor
    boolean  runFair

    private volatile Pair<FQueue<Action<R,Object>>,FList<R>> state = [FQueue.emptyQueue,null]

    /**
     * @return created pooled resources
     */
    abstract Iterable<R> initResources ()

    abstract static class Action<R,D> extends Function1<R,D> {
        Function1<D,Object> whenDone
    }

    final <D> void execute (Action<R,D> action, Function1<D,Object> whenDone = null) {
        action.whenDone = whenDone
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

    private final <D> Object scheduledAction(Action<R,D> action, R resource) {
        def res = action(resource)

        for (;;) {
            def s = state
            if (s.first.empty) {
                // no more actions => we return resource to the pool
                if(state.compareAndSet(s, [FQueue.emptyQueue, s.second + resource])) {
                    return action.whenDone?.call (res)
                }
            }
            else {
                def removed = s.first.removeFirst()
                if(state.compareAndSet(s, [removed.second, s.second])) {
                    if (runFair || action.whenDone) {
                        // schedule action
                        executor.execute {
                            scheduledAction(removed.first,resource)
                        }

                        return action.whenDone?.call (res)
                    }
                    else {
                        // tail recursion
                        return scheduledAction(removed.first, resource)
                    }
                }
            }
        }
    }

    private synchronized void initPool () {
        if (state.second == null) {
            state.second = FList.emptyList.addAll(initResources ())
        }
    }
}
