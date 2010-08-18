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

package org.mbte.groovypp.examples;

import groovy.util.GroovyTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.Arrays;

public class PiJava extends GroovyTestCase {

    public void test1 () {
        for (int i = 10; i > 0; i--) {
            calc(i);
            System.out.println ();
        }
    }

    public void calc ( int actorCount ) {
      long n = 100000000l; // 10 times fewer due to speed issues.
      final double delta = 1.0d / n;
      double sliceSize = n / actorCount;
      long startTimeNanos = System.nanoTime ( );

      final CountDownLatch cdl = new CountDownLatch(actorCount);

      final double [] data = new double [actorCount];
      Arrays.fill(data, 0d);

      for(int index = 0; index != actorCount; ++index) {
        final long start = (long )(index * sliceSize);
        final long end = (long)(( index + 1l ) * sliceSize -1);
          final int index1 = index;
          new Thread(new Runnable(){
            public void run() {
                calculation(start, end, delta, data, index1, cdl);
            }
        }).start ();
      }

        try {
            cdl.await ();
        } catch (InterruptedException e) {//
        }

        double sum = 0d;
      for(int index = 0; index != actorCount; ++index) {
          sum += data[index];
      }

      final double pi = 4.0d * sum * delta;
      final double elapseTime = ( System.nanoTime ( ) - startTimeNanos ) / 1e9;
      System.out.println("==== Groovy GPars ActorScript pi = " + pi );
      System.out.println("==== Groovy GPars ActorScript iteration count = " + n );
      System.out.println("==== Groovy GPars ActorScript elapse = " + elapseTime );
      System.out.println("==== Groovy GPars ActorScript actor count = " + actorCount );
    }

    public static void calculation(long start, long end, double delta, double[] data, int index1, CountDownLatch cdl) {
        double sum = 0.0d;
        for ( long i = start ; i <= end ; ++i ) {
          double x = ( i - 0.5d ) * delta;
          sum += 1.0d / ( 1.0d + x * x );
        }
        data [index1] = sum;
        cdl.countDown();
    }
}