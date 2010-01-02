package groovy.util

import java.util.concurrent.Executor
import java.util.concurrent.FutureTask
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport

/**
 * Utility methods to map between objects of standard types.
 */
@Typed
public class Mappers extends DefaultGroovyMethodsSupport {

  private def Mappers() {}

  /**
   * Creates the collection containing the results of op application to the elements of the original collection.
   * @param self original collection.
   * @param op mapping function.
   * @return collection containing the results of application.
   */
  static <T, R> Collection<R> map(Iterable<T> self, Function1<T, R> op) {
    def res = (Collection<R>) (self instanceof Collection ? createSimilarCollection((Collection)self) : new ArrayList<R>())
    for (T t: self) {
      res << op[t]
    }
    res
  }

  /**
   * Creates the array containing the results of op application to the elements of the original array.
   * @param self original array.
   * @param op mapping function.
   * @return array containing the results of application.
   */
  static <T, R> R[] map(T[] self, Function1<T, R> op) {
    def res = (R[]) new Object[self.length]
    for (int i = 0; i < res.length; i++) {
      res[i] = op[self[i]]
    }
    res
  }

  /**
   * Creates the iterator containing the results of op application to the elements returned by the original iterator.
   * @param self original iterator.
   * @param op mapping function.
   * @return iterator returning the results of application.
   */
  static <T, R> Iterator<R> map(Iterator<T> self, Function1<T, R> op) {
    [next: { op[self.next()] }, hasNext: { self.hasNext() }, remove: { self.remove() }]
  }

  /**
   * Creates the 'zip' of two iterators, i.e. the iterator containing pairs of elements from two iterators, while
   * both of them produce elements.
   * @param self original iterator.
   * @param other iterator to zip with.
   * @return Iterator of pairs of elements from each zipped iterators.
   */
  static <T1, T2> Iterator<Pair<T1, T2>> zip(Iterator<T1> self, Iterator<T2> other) {
    [next: { [self.next(), other.next()] },
            hasNext: { self.hasNext() && other.hasNext() },
            remove: { throw new UnsupportedOperationException("remove() is not supported") }]
  }

  private static <T1, T2> Iterator<Pair<T1, T2>> product(Iterator<T1> self, Function1<T1, Iterator<T2>> op) {
    [first: (T1) null,

     second: (Iterator<T2>) null,

     hasNext: {
        second || self && (second = op[first = self.next()])
     },

     next: { [first, second.next()] },

     remove: { throw new UnsupportedOperationException("remove() is not supported") }]
  }

  /**
   * Obtains the Cartesian product of two @link{Iterable} objects. Note that @link{Iterable#iterator()} is called
   * multiple times for the second argument.
   * @param self first Iterable.
   * @param other second Iterable.
   * @return Iterator to Cartesian product.
   */
  static <T1,T2> Iterator<Pair<T1,T2>> product (Iterable<T1> self, Iterable<T2> other) {
    self.iterator().product { other.iterator() }
  }

  /**
   * Group the elements of the input collection according to the grouping function.
   * @param self input collection.
   * @param op grouping function.
   * @return @link{java.util.Map} associating keys to lists of elements in the original collection,
   * matching that key.
   */
  @Typed
  static <T, K> Map<K, List<T>> groupBy(Collection<T> self, Function1<T, K> op) {
    def answer = (Map<K, List<T>>) [:]
    for (T element: self) {
      def value = op.call(element)
      def list = answer.get(value)
      if (list == null) {
        list = new LinkedList<T>()
        answer[value] = list
      }
      list << element
    }
    answer
  }

    /**
     * Group the elements of the input collection according to the grouping function.
     * @param self input collection.
     * @param op grouping function.
     * @return @link{java.util.Map} associating keys to lists of elements in the original collection,
     * matching that key.
     */
    @Typed
    static <T, K> Map<K, List<T>> groupBy(Iterator<T> self, Function1<T, K> op) {
      def answer = (Map<K, List<T>>) [:]
      for (element in self) {
        def value = op.call(element)
        def list = answer.get(value)
        if (list == null) {
          list = new LinkedList<T>()
          answer[value] = list
        }
        list << element
      }
      answer
    }

  /**
   * Flattens the results of applying the mapping function to each element of input iterator.
   * @param self input iterator of iterators.
   * @param op mapping function.
   * @return mapped and flattened iterator.
   */
  static <T, R> Iterator<R> flatMap(Iterator<Iterator<T>> self, Function1<T, R> op) {
    [
            curr: (Iterator<T>) null,
            hasNext: {
              (curr != null && curr.hasNext()) || (self.hasNext() && (curr = self.next()).hasNext())
            },
            next: { op.call(curr.next()) },
            remove: { throw new UnsupportedOperationException("remove() is not supported") }
    ]
  }

  /**
   * Flattens the results of applying the mapping function to each element of input @link{Iterable} object.
   * @param self input Iterable, e.g. List<List>.
   * @param op mapping function.
   * @return mapped and flattened iterator.
   */
  static <T, R> Iterator<R> flatMap(Iterable<Iterable<T>> self, Function1<T, R> op) {
    flatMap(self.iterator().map {it.iterator()}, op)
  }

  /**
   * Flattens the input iterator.
   * @param self input iterator of iterators.
   * @return flattened iterator.
   */
  static <T> Iterator<T> flatten(Iterator<Iterator<T>> self) {
    flatMap(self, {it})
  }

  /**
   * Flattens the input @link{Iterable}.
   * @param self input Iterable, e.g. List<List>.
   * @return flattened iterator.
   */
  static <T> Iterator<T> flatten(Iterable<Iterable<T>> self) {
    flatMap(self, {it})
  }

  static <T, R> Iterator<R> mapConcurrently(Iterator<T> self,
                                            Executor executor,
                                            boolean ordered,
                                            int maxConcurrentTasks = 0,
                                            Function1<T, R> op) {
    int processors = Runtime.runtime.availableProcessors()
    if (maxConcurrentTasks < processors)
      maxConcurrentTasks = 2 * processors

    if (ordered) {
      [pending: 0,
              waiting: new LinkedList<FutureTask<R>>(),

              testPending: {->
                while (self && pending < maxConcurrentTasks) {
                  pending++
                  waiting << op.future(self.next())
                  executor.execute waiting.last
                }
              },

              next: {->
                def res = waiting.removeFirst().get()
                pending--
                testPending()
                res
              },

              hasNext: {-> testPending(); pending > 0 },

              remove: {-> throw new UnsupportedOperationException("remove () is unsupported by the iterator") },
      ]
    }
    else {
      [   pending: 0,
          ready: new LinkedBlockingQueue<R>(),

          scheduleIfNeeded: {->
            while (self && pending < maxConcurrentTasks) {
              pending++
              def nextElement = self.next()
              executor.execute {-> ready << op.call(nextElement) }
            }
          },

          next: {->
            def res = ready.take()
            pending--
            scheduleIfNeeded()
            res
          },

          hasNext: {-> scheduleIfNeeded(); pending > 0 },

          remove: {-> throw new UnsupportedOperationException("remove () is unsupported by the iterator") },
      ]
    }
  }
}
