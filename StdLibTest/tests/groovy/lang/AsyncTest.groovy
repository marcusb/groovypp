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



package groovy.lang

public class AsyncTest extends GroovyShellTestCase {
    void testAsync () {
        shell.evaluate """
            @Typed package p

            import java.util.concurrent.*
            import groovy.util.concurrent.*

            @Async int calculation (int a, int b) {
               a + b
            }

            testWithFixedPool {
                CountDownLatch cdl = [1]
                assert 21 == calculation (10, 11, pool){ bl ->
                    assert 21 == bl.get()
                    cdl.countDown ()
                }.get ()
                assert cdl.await(10L,TimeUnit.SECONDS)
            }
        """
    }
}