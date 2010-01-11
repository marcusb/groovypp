package groovy.util

import java.util.concurrent.TimeUnit

/**
 * Stream of non-null objects
 */
interface SupplyStream<T> {
    /**
     *  @return next element or null
     */
    T take () throws InterruptedException

    /**
     *  @return next element or null
     */
    T poll(long timeout, TimeUnit unit) throws InterruptedException
}