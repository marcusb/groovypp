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

package groovy.util

import groovy.util.concurrent.BindLater
import groovy.util.concurrent.FList
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import org.codehaus.groovy.runtime.DefaultGroovyMethods

/**
 * Utility methods to iterate over objects of standard types.
 */
@Typed
abstract class Iterations {
    /**
     * Computes the aggregate of the given iterator applying the operation to the first iterator element first.
     * @param self input iterator.
     * @param init the initial value to be passed on op first invocation.
     * @param op the function that computes aggregate given the current element and its own result computed so far.
     * @return computed aggregated value.
     */
    @GrUnit({ assertEquals (8, [1,2,3].iterator().foldLeft(2){ sum, value -> sum + value }) })
    static <T, R> R foldLeft(Iterator<T> self, R init, Function2<T, R, R> op) {
        self.hasNext() ? foldLeft(self, op.call(self.next(), init), op) : init
    }

    /**
     * Computes the aggregate of the given iterator applying the operation to the first element first.
     * @param self input @link{Iterable} object.
     * @param init the initial value to be passed on op first invocation.
     * @param op the function that computes aggregate given the current element and its own result computed so far.
     * @return computed aggregated value.
     */
    @GrUnit({ assertEquals (8, [1,2,3].foldLeft(2){ sum, value -> sum + value }) })
    static <T, R> R foldLeft(Iterable<T> self, R init, Function2<T, R, R> op) {
        foldLeft(self.iterator(), init, op)
    }

    @GrUnit({ assertEquals (8, ([1,2,3] as Integer[]).iterator().foldLeft(2){ sum, value -> sum + value }) })
    static <T, R> R foldLeft(T[] self, R init, Function2<T, R, R> op) {
        foldLeft(self.asList(), init, op)
    }

    /**
     * Computes the aggregate of the given iterator applying the operation to the last iterator element first.
     * @param self input iterator.
     * @param init the initial value to be passed on op first invocation.
     * @param op the function that computes aggregate given the current element and its own result computed so far.
     * @return computed aggregated value.
     */
    static <T, R> R foldRight(Iterator<T> self, R init, Function2<T, R, R> op) {
      def rev = FList.emptyList
      while(self.hasNext()) {
        rev = rev + self.next()
      }
      foldLeft(rev.iterator(), init, op)
    }

    /**
     * Computes the aggregate of the given iterator applying the operation to the last element first.
     * @param self input @link{Iterable} object.
     * @param init the initial value to be passed on op first invocation.
     * @param op the function that computes aggregate given the current element and its own result computed so far.
     * @return computed aggregated value.
     */
    static <T, R> R foldRight(Iterable<T> self, R init, Function2<T, R, R> op) {
        foldRight(self.iterator(), init, op)
    }

    static <T, R> R foldRight(T[] self, R init, Function2<T, R, R> op) {
        foldRight(self.asList(), init, op)
    }

    /**
     * Applies given function to each iterator element. The result of function application is discarded.
     * @param self input iterator.
     * @param op function to be applied.
     */
    static <T> void each(Iterator<T> self, Function1<T, Object> op) {
        while (self.hasNext()) op.call(self.next())
    }

    /**
     * Iterates through this object transforming each value into a new value using the
     * closure as a transformer, returning a list of transformed values.
     * Example:
     * <pre class="groovyTestCase">def list = [1, 'a', 1.23, true ]
     * def types = list.collect { it.class }
     * assert types == [Integer, String, BigDecimal, Boolean]</pre>
     *
     * @param self    the values of the object to transform
     * @param closure the closure used to transform each element of the collection
     * @return a Collection of the transformed values
     */
    public static <T,R> Collection<R> collect(Iterator<T> self, Collection<R> where = [], Function1<T,R> closure) {
        for(el in self)
            where << closure(el)
        where
    }

    /**
     * Applies given function to each iterator element. The result of function application is discarded.
     * @param self input iterator.
     * @param op function to be applied.
     */
    static <T> BindLater each(Iterator<T> self, Executor executor, int concurrency = 0, Function1<T, Object> op) {
        if (!concurrency)
            concurrency = Runtime.getRuntime().availableProcessors()

        BindLater.Group result = [concurrency]
        for (j in 0..concurrency) {
            result.attach(executor.callLater {
                while(!result.isDone()) {
                    T el
                    synchronized (self) {
                        if (!self)
                            break

                        el = self.next()
                    }

                    op [el]
                }
            })
        }
        result
    }

    /**
     * Used to determine if the given predicate fucntion is valid (i.e.&nsbp;returns
     * <code>true</code> for all items in this data structure).
     * A simple example for a list:
     * <pre>def list = [3,4,5]
     * def greaterThanTwo = list.every { it > 2 }
     * </pre>
     *
     * @param self the Iterator over which we iterate
     * @param closure the function predicate used for matching
     * @return true if every iteration of the object matches the closure predicate
     */
    static <T> boolean every(Iterator<T> self, Function1<T, Boolean> closure) {
        for (el in self) {
            if (!closure.call(el))
                return false
        }
        return true
    }

    /**
     * Applies given function to each element. The result of function application is discarded.
     * @param self input @link{Iterable} object.
     * @param op function to be applied.
     */
    static <T> void each(Iterable<T> self, Function1<T, Object> op) {
        each(self.iterator(), op)
    }


    /**
     * Iterates through this object transforming each value into a new value using the
     * closure as a transformer, returning a list of transformed values.
     * Example:
     * <pre class="groovyTestCase">def list = [1, 'a', 1.23, true ]
     * def types = list.collect { it.class }
     * assert types == [Integer, String, BigDecimal, Boolean]</pre>
     *
     * @param self    the values of the object to transform
     * @param closure the closure used to transform each element of the collection
     * @return a Collection of the transformed values
     */
    public static <T,R> Collection<R> collect(Iterable<T> self, Collection<R> where = [], Function1<T,R> closure) {
        for(el in self)
            where << closure(el)
        where
    }

    /**
     * Used to determine if the given predicate fucntion is valid (i.e.&nsbp;returns
     * <code>true</code> for all items in this data structure).
     *
     * @param self the Iterable over which we iterate
     * @param closure the function predicate used for matching
     * @return true if every iteration of the object matches the closure predicate
     */
    static <T> boolean every(Iterable<T> self, Function1<T, Boolean> closure) {
        every(self.iterator(), closure)
    }

    /**
     * Applies given function to each array element. The result of function application is discarded.
     * @param self input array.
     * @param op function to be applied.
     */
    static <T> void each(T[] self, Function1<T, Object> op) {
        each(self.iterator(), op)
    }

    /**
     * Iterates through this object transforming each value into a new value using the
     * closure as a transformer, returning a list of transformed values.
     * Example:
     * <pre class="groovyTestCase">def list = [1, 'a', 1.23, true ]
     * def types = list.collect { it.class }
     * assert types == [Integer, String, BigDecimal, Boolean]</pre>
     *
     * @param self    the values of the object to transform
     * @param closure the closure used to transform each element of the collection
     * @return a Collection of the transformed values
     */
    public static <T,R> Collection<R> collect(T [] self, Collection<R> where = [], Function1<T,R> closure) {
        for(el in self)
            where << closure(el)
        where
    }

    /**
     * Used to determine if the given predicate fucntion is valid (i.e.&nsbp;returns
     * <code>true</code> for all items in this data structure).
     *
     * @param self the list over which we iterate
     * @param closure the function predicate used for matching
     * @return true if every iteration of the object matches the closure predicate
     */
    static <T> boolean every(T[] self, Function1<T, Boolean> closure) {
        every(self.iterator(), closure)
    }

    /**
     * Applies given function to each enumeration element. The result of function application is discarded.
     * @param self input {java.util.Enumeration} object.
     * @param op function to be applied.
     */
    static <T> void each(Enumeration<T> self, Function1<T, Object> op) {
        each(self.iterator(), op)
    }

    /**
     * Iterates through this object transforming each value into a new value using the
     * closure as a transformer, returning a list of transformed values.
     * Example:
     * <pre class="groovyTestCase">def list = [1, 'a', 1.23, true ]
     * def types = list.collect { it.class }
     * assert types == [Integer, String, BigDecimal, Boolean]</pre>
     *
     * @param self    the values of the object to transform
     * @param closure the closure used to transform each element of the collection
     * @return a Collection of the transformed values
     */
    public static <T,R> Collection<R> collect(Enumeration<T> self, Collection<R> where = [], Function1<T,R> closure) {
        for(el in self)
            where << closure(el)
        where
    }

    /**
     * Used to determine if the given predicate fucntion is valid (i.e.&nsbp;returns
     * <code>true</code> for all items in this data structure).
     *
     * @param self the Enumeration over which we iterate
     * @param closure the function predicate used for matching
     * @return true if every iteration of the object matches the closure predicate
     */
    static <T> boolean every(Enumeration<T> self, Function1<T, Boolean> closure) {
        every(self.iterator(), closure)
    }

    /**
     * Applies given function to each map element. The result of function application is discarded.
     * @param self input {java.util.Map} object.
     * @param op function to be applied.
     */
    static <K, V> void each(Map<K, V> self, Function2<K, V, Object> op) {
        def it = self.entrySet().iterator()
        while (it.hasNext()) {
            def el = it.next()
            op.call(el.key, el.value)
        }
    }

    /**
     * Iterates through this object transforming each value into a new value using the
     * closure as a transformer, returning a list of transformed values.
     * Example:
     * <pre class="groovyTestCase">def list = [1, 'a', 1.23, true ]
     * def types = list.collect { it.class }
     * assert types == [Integer, String, BigDecimal, Boolean]</pre>
     *
     * @param self    the values of the object to transform
     * @param closure the closure used to transform each element of the collection
     * @return a Collection of the transformed values
     */
    public static <K,V,R> Collection<R> collect(Map<K,V> self, Collection<R> where = [], Function2<K,V,R> closure) {
        for(el in self.entrySet())
            where << closure(el.key, el.value)
        where
    }

    /**
     * Used to determine if the given predicate fucntion is valid (i.e.&nsbp;returns
     * <code>true</code> for all items in this data structure).
     *
     * @param self    the map over which we iterate
     * @param closure the function predicate used for matching
     * @return true if every iteration of the object matches the closure predicate
     */
    static <K, V> boolean every(Map<K,V> self, Function1<Map.Entry<K, V>, Boolean> closure) {
        every(self.entrySet().iterator(), closure)
    }

    /**
     * Used to determine if the given predicate fucntion is valid (i.e.&nsbp;returns
     * <code>true</code> for all items in this data structure).
     *
     * @param self    the map over which we iterate
     * @param closure the function predicate used for matching
     * @return true if every iteration of the object matches the closure predicate
     */
    static <K, V> boolean every(Map<K,V> self, Function2<K, V, Boolean> closure) {
        for (el in self.entrySet()) {
            if (!closure.call(el.key, el.value))
                return false
        }
        true
    }

    /**
     * Default dynamic iteration.
     */
    static <T> T each(T self, Closure closure) {
        return DefaultGroovyMethods.each(self, closure)
    }

    /**
     * Default dynamic iteration.
     */
    static <T> List collect(T self, Closure closure) {
        return DefaultGroovyMethods.collect(self, closure)
    }

	/**
	 * Default dynamic iteration.
	 */
	static <T> boolean every(T self, Closure closure) {
	    return DefaultGroovyMethods.every(self, closure)
	}


    static <T> BlockingQueue<T> leftShift(BlockingQueue<T> queue, Iterator<T> iter) {
        for (el in iter) {
            queue << iter
        }
    }

    static <T> SupplyStream<T> supply(Iterator<T> iterator) {
        [
            take: { synchronized (iterator) { iterator ? iterator.next() : null } },
            poll: {long timeout, TimeUnit unit -> take () }
        ]
    }

    static <T> SupplyStream<T> supply(Iterable<T> iterable) {
        iterable.iterator().supply ()
    }
}
