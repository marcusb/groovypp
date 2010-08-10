package org.mbte.gretty.redis

import java.util.concurrent.CountDownLatch
import groovy.util.concurrent.ResourcePool
import java.util.concurrent.Semaphore
import java.util.concurrent.Executors

@Typed class RedisTestManual extends GroovyTestCase {

    RedisClient redis

    protected void setUp() {
        super.setUp()

        redis = [new InetSocketAddress('localhost',6379)]

        CountDownLatch cdl = [1]
        redis.connect {
            println ">>> ${it.success}"
            if(!it.success) {
                redis = null
            }
            else {
//                redis.flushDb().get()
            }
            cdl.countDown()
        }
        cdl.await()
    }

    protected void tearDown() {
        redis?.disconnect()
        super.tearDown()
    }

    void testGetSet() {
        if(!redis)
            return

        def pool = Executors.newFixedThreadPool(Runtime.runtime.availableProcessors())

        def n = 100000
        CountDownLatch cdl = [n]
        def start = System.currentTimeMillis()
        for(i in 0..<n) {
            def masterString = "lambada\r\ngranada - $i"
            def  upperString = masterString.toUpperCase()
            def key = "$i"

            redis.set(key,masterString){ boundSet ->
                if(i+1 == n)
                    println "last set: ${System.currentTimeMillis() - start}"
                redis.getset(key, upperString){ boundGet ->
                    if(i+1 == n)
                        println "last getset: ${System.currentTimeMillis() - start}"
                    assertEquals masterString, boundGet.get()
                }
                redis.get(key){ boundGet ->
                    if(i+1 == n)
                        println "last get: ${System.currentTimeMillis() - start}"
                    assertEquals upperString, boundGet.get()
                }
                def newKey = "new$key"
                redis.rename(key,newKey){ boundGet ->
                    if(i+1 == n)
                        println "last rename: ${System.currentTimeMillis() - start}"
                    assertTrue boundGet.get()
                }
                redis.exists(key){ exists ->
                    if(i+1 == n)
                        println "last exists 1: ${System.currentTimeMillis() - start}"
                    assertFalse exists.get ()
                }
                redis.exists(newKey){ exists ->
                    if(i+1 == n)
                        println "last exists 2: ${System.currentTimeMillis() - start}"
                    assertTrue exists.get ()
                }
                redis.del([key, newKey]){ del ->
                    if(i+1 == n)
                        println "last del: ${System.currentTimeMillis() - start}"
                    assertEquals 1, del.get ()
                }
                redis.exists(newKey){ exists ->
                    if(i+1 == n)
                        println "last exists 3: ${System.currentTimeMillis() - start}"
                    assertFalse exists.get ()
                    cdl.countDown()
                }
            }
        }

        cdl.await()

        redis.flushDb().get()
    }

    void testMgetMset () {
        if(!redis)
            return

        redis.mset([0, "0", 1, "1", 4, ["15", "25"]])
        assertEquals ([["15", "25"], null, "1", "0"], redis.mget(["4", "8", "1", "0"]).get())

        redis.flushDb().get()
    }
}