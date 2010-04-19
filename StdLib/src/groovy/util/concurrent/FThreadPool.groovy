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

@Typed class FThreadPool implements Executor {

  protected volatile FQueue<Runnable> queue = FQueue.emptyQueue

  private static final Runnable stopMarker = {}

  private final Semaphore semaphore = [0]

  FThreadPool(int num = Runtime.getRuntime().availableProcessors(), ThreadFactory threadFactory = Executors.defaultThreadFactory()) {
    for(i in 0..<num) {
      def thread = threadFactory.newThread {
        for(;;) {
          semaphore.acquire()

          for(;;) {
            def q = queue

            if(q.first === stopMarker) {
              runStopping()
              return
            }
            else {
              if(tryRun(q))
                break
            }
          }
        }
      }
      thread.start()
    }
  }

  private boolean tryRun(FQueue<Runnable> q) {
    def got = q.removeFirst()
    if(queue.compareAndSet(q, got.second)) {
      got.first.run()
      return true
    }
    return false
  }

  private void runStopping() {
    for (;;) {
      def q = queue

      if(q.size() == 1) {
        return
      }
      else {
        def got = q.removeFirst().second.removeFirst()
        if(queue.compareAndSet(q, got.second.addFirst(stopMarker))) {
          got.first.run()
        }
      }
    }
  }

  void execute(Runnable command) {
    for(;;) {
      def q = queue
      if (!q.empty && q.first == stopMarker)
        throw new RejectedExecutionException()

      if(queue.compareAndSet(q, q + command)) {
        semaphore.release()
        break
      }
    }
  }

  /**
   * Initiate process of shutdown
   * No new tasks can be scheduled after that point
   */
  void shutdown() {
    for(;;) {
      def q = queue
      if (q.empty) {
        if(queue.compareAndSet(q, FQueue.emptyQueue + stopMarker)) {
          break
        }
      }
      else {
        if (q.first !== stopMarker) {
          if(queue.compareAndSet(q, q.addFirst(stopMarker))) {
            break
          }
        }
      }
    }
    semaphore.release(Integer.MAX_VALUE)
  }

  /**
   * Initiate process of shutdown
   * No new tasks can be scheduled after that point and all tasks, which not started execution yet, will be unscheduled
   */
  List<Runnable> shutdownNow() {
    for(;;) {
      def q = queue
      if(queue.compareAndSet(q, FQueue.emptyQueue + stopMarker)) {
        semaphore.release(Integer.MAX_VALUE)
        return q.iterator().asList()
      }
    }
  }

  boolean awaitTermination(long timeout, TimeUnit timeUnit) {
    true
  }
}
