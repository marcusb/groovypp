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



package org.mbte.groovypp.examples

import java.util.concurrent.CountDownLatch

@Typed
public class Pi extends GroovyTestCase {

    void test1 () {
        for (int i = 10; i > 0; i--) {
            calc(i)
            println ()
        }
    }

    private def calc ( int actorCount ) {
        
      long   n = 100000000l // 10 times fewer due to speed issues.
      double delta = 1.0d / n
      double sliceSize = n / actorCount
      long   startTimeNanos = System.nanoTime ( )

      def cdl = new CountDownLatch(actorCount)

      def data = new double [actorCount]
      data.fill 0d

      startThreads(sliceSize, actorCount, delta, data, cdl)

      cdl.await ()

      def sum = 0d
      for(int index = 0; index != actorCount; ++index) {
          sum += data[index]
      }

      printResults(actorCount, n, startTimeNanos, delta, sum)
    }

    private def printResults(int actorCount, long n, long startTimeNanos, double delta, double sum) {
        final double pi = 4.0d * sum * delta
        final double elapseTime = (System.nanoTime() - startTimeNanos) / 1e9
        println("==== Groovy GPars ActorScript pi = " + pi)
        println("==== Groovy GPars ActorScript iteration count = " + n)
        println("==== Groovy GPars ActorScript elapse = " + elapseTime)
        println("==== Groovy GPars ActorScript actor count = " + actorCount)
    }

    private void startThreads(double sliceSize, int actorCount, double delta, double [] data, CountDownLatch cdl) {
        for (int index = 0; index != actorCount; ++index) {
            long start = index * sliceSize
            long end = (index + 1l) * sliceSize - 1
            new Thread((Runnable){
                double sum = 0.0d, x
                for (long i = start; i != end; ++i) {
                    x = (i - 0.5d) * delta
                    sum += 1.0d / (1.0d + x * x)
                }
                data[index] = sum
                cdl.countDown()
            }).start()
        }
    }
}
