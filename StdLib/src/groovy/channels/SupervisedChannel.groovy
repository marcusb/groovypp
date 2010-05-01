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

import java.util.concurrent.atomic.AtomicInteger
import groovy.util.concurrent.FList

@Typed abstract class SupervisedChannel<M> extends ExecutingChannel<M> {
    private static int NOT_STARTED = 0
    private static int STARTING = 1
    private static int STARTED = 2
    private static int STOP_MASK = 8
    private static int STOPPING = STOP_MASK
    private static int STOPPED = STOP_MASK | 1
    private static int CRASHED = STOP_MASK | 2

    SupervisedChannel owner

    private volatile FList<SupervisedChannel> children = FList.emptyList

    private volatile int state

    private void fireStartup (Function0 afterStartup) {
        schedule {
            doStartup()
            afterStartup?.call()
            state = STARTED
        }
    }

    final void startup (Function0 afterStartup = null) {
        if (owner) {
            owner.startupChild(this, afterStartup)
        }
        else {
            beginStartup()
            if (!executor) throw new NullPointerException("executor should be set prior to startup")
            fireStartup afterStartup
        }
    }

    final void startupChild(SupervisedChannel child, Function0 afterStartup = null) {
        child.beginStartup()
        child.owner = this
        children.apply { it + child }
        child.executor = child.executor ? child.executor : this.executor
        child.fireStartup afterStartup
    }

    private void beginStartup() {
        if (!state.compareAndSet(0, STARTING)) {
            throw new IllegalStateException("${this} was started already")
        }
    }

    private void fireShutdown(Function0 afterShutdown) {
        schedule {
            def childsCount = children.size()
            if (childsCount) {
                AtomicInteger cnt = [childsCount]
                for (c in children)
                    c.shutdown {
                        if (!cnt.decrementAndGet()) {
                            doShutdown()
                            afterShutdown?.call()
                            state = STOPPED
                        }
                    }
            }
            else {
                doShutdown()
                afterShutdown?.call()
                state = STOPPED
            }
        }
    }

    final void shutdown(Function0 afterShutdown = null) {
        if (owner)
            owner.shutdownChild(this, afterShutdown)
        else {
            beginShutdown()
            fireShutdown afterShutdown
        }
    }

    private void beginShutdown() {
        for (;;) {
            def s = state
            switch (s) {
                case NOT_STARTED:
                    throw new IllegalStateException("${this} was not started yet")

                case STOPPING:
                case STOPPED:
                    throw new IllegalStateException("${this} was stopped already")

                case STARTING:
                case STARTED:
                    if (state.compareAndSet(s, STOPPING)) {
                        return
                    }
            }
        }
    }

    final void shutdownChild(SupervisedChannel child, Function0 afterShutdown = null) {
        child.beginShutdown()
        children.apply { it - child }
        child.fireShutdown afterShutdown
    }

    protected final void onMessage(Object message) {
        try {
            doOnMessage(message)
        }
        catch (Throwable cause) {
            cause.printStackTrace()
            crash(cause)
        }
    }

    private void fireChildCrashed (SupervisedChannel who, Throwable reason) {
        schedule {
            shutdownChild(who)
            onChildCrashed(who, reason)
        }
    }

    protected void doOnMessage(Object message) {
        super.onMessage message
    }

    protected void doStartup() {}

    protected void doShutdown() {}

    protected void onChildCrashed(SupervisedChannel who, Throwable reason) {
        throw reason
    }

    protected final void crash(Throwable cause) {
        for (c in children)
            c.shutdown()

        if (owner)
            owner.fireChildCrashed(this, cause)
        else
            throw cause
    }

    final SupervisedChannel getRootSupervisor() {
        owner ? owner.rootSupervisor : this
    }

    final boolean stopped () {
        state & STOP_MASK
    }
}
