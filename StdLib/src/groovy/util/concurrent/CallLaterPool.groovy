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

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

@Typed
class CallLaterPool extends ThreadPoolExecutor {
    CallLaterPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new DefaultThreadFactory())
        ((DefaultThreadFactory)getThreadFactory()).pool = this
    }

    protected static class GroovyThread extends Thread {
       final ExecutorService pool

       GroovyThread (ExecutorService pool, ThreadGroup group, Runnable r, String name) {
           super(group, r, name, 0)
           this.@pool = pool
           setPriority(Thread.NORM_PRIORITY)
           setDaemon(false)
       }
    }

    protected static class DefaultThreadFactory implements ThreadFactory {
        static final AtomicInteger poolNumber = [1]

        final AtomicInteger threadNumber = [1]
        final ThreadGroup group
        final String namePrefix

        CallLaterPool pool

        DefaultThreadFactory() {
            def s = System.getSecurityManager();
            this.@group = s ? s.getThreadGroup() : Thread.currentThread().threadGroup
            this.@namePrefix = "groovy-pool-" + poolNumber.getAndIncrement() + "-thread-"
        }

        GroovyThread newThread(Runnable r) {
            [pool, group, r, namePrefix + threadNumber.getAndIncrement()]
        }
    }
}
