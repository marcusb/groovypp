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
  static <T, R> Collection<R> map(Collection<T> self, Function1<T, R> op) {
    def res = (Collection<R>) createSimilarCollection(self)
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
   * Obtains the Cartesian product of two @link{Iterable} objects. Note that @link{Iterable#getIterator()} is called
   * multiple times for the second argument.
   * @param self first iterator.
   * @param other second iterator
   * @return Iterator to Cartesian product.
   */
  static <T1,T2> Iterator<Pair<T1,T2>> product (Iterable<T1> self, Iterable<T2> other) {
    self.iterator().product { other.iterator() }
  }

  static <T, K> Map<K, List<T>> groupBy(Collection<T> self, Function1<T, K> op) {
    def answer = (Map<K, List<T>>) [:]
    for (T element: self) {
      def value = op.apply(element)
      def list = answer.get(value)
      if (list == null) {
        list = new LinkedList<T>()
        answer[value] = list
      }
      list << element
    }
    answer
  }

  static <T, R> Iterator<R> flatMap(Iterator<Iterator<T>> self, Function1<T, R> op) {
    [
            curr: (Iterator<T>) null,
            hasNext: {
              (curr != null && curr.hasNext()) || (self.hasNext() && (curr = self.next()).hasNext())
            },
            next: { op.apply(curr.next()) },
            remove: { throw new UnsupportedOperationException("remove() is not supported") }
    ]
  }

  static <T, R> Iterator<R> flatMap(Iterable<Iterable<T>> self, Function1<T, R> op) {
    flatMap(self.iterator().map {it.iterator()}, op)
  }

  static <T> Iterator<T> flatten(Iterator<Iterator<T>> self) {
    flatMap(self, {it})
  }

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
      [pending: new AtomicInteger(),
              waiting: new LinkedBlockingQueue<R>(),

              testPending: {->
                while (self && waiting.size() + pending.get() < maxConcurrentTasks) {
                  pending.incrementAndGet()
                  def call = op.curry(self.next())
                  executor.execute {-> waiting << call.apply(); pending.decrementAndGet() }
                }
              },

              next: {->
                def res = waiting.take()
                testPending()
                res
              },

              hasNext: {-> testPending(); !waiting.empty || pending.get() > 0 },

              remove: {-> throw new UnsupportedOperationException("remove () is unsupported by the iterator") },
      ]
    }
  }
}
