package org.mbte.gretty.redis

import java.util.concurrent.CountDownLatch

@Typed abstract class RedisTestBase extends GroovyTestCase {
    private static boolean redisPresent = true

    protected RedisClient redis

    protected void setUp() {
        super.setUp()

        if(redisPresent) {
            redis = [new InetSocketAddress('localhost',6379)]
            def connect = redis.connect().awaitUninterruptibly(5000)
            if(!connect || !redis.connected) {
                redis?.disconnect() // in unlikely case of timeout
                redis = null
            }
        }
    }

    protected void tearDown() {
        redis?.disconnect()
        super.tearDown()
    }
}
