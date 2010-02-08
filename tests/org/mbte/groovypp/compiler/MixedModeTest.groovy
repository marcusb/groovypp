package org.mbte.groovypp.compiler

public class MixedModeTest extends GroovyShellTestCase {

  void testMe() {
    def res = shell.evaluate("""
    import groovy.xml.*

    @Typed(value=TypePolicy.MIXED)
    class A {
        void m () {
            def writer = new StringWriter()
            def mb = new MarkupBuilder (writer);
            def i = 0
            mb."do" {
     //           a(i){
                    Integer j = i
                    while (!(j++ == 5)) {
                        b("b\$j")
                    }
    //            }
    //            c {
    //            }
            }
            writer.toString ()
        }
    }

    new A ().m ()
""")
  }

    void testSequentially () {
        shell.evaluate """
          import java.util.concurrent.*
          import java.util.concurrent.atomic.*
          import groovy.xml.*


          @Typed(TypePolicy.MIXED)
          void u () {
              new MarkupBuilder ().numbers {
                  def divisors = { int n, Collection alreadyFound = [] ->
                      if (n > 3)
                          for(candidate in 2..<n)
                             if (n % candidate == 0)
                                return call (n / candidate, alreadyFound << candidate)

                      alreadyFound << n
                  }

                  (2..1500).iterator().mapConcurrently(Executors.newFixedThreadPool(10), 50) {
                     new Pair(it, divisors(it))
                  }.each { pair ->
                    if (pair.second.size () == 1)
                        number ([value: pair.first, prime:true ])
                    else {
                        number ([value: pair.first, prime:false]) {
                          divisor([value: pair])
                        }
                    }
                  }
              }
          }

          u ()
        """
    }

  void testConcurrently () {
      shell.evaluate """
        import java.util.concurrent.*
        import java.util.concurrent.atomic.*
        import groovy.xml.*

        static <T,R> Iterator<R> mapConcurrently (Iterator<T> self, Executor executor, int maxConcurrentTasks, Function1<T,R> op) {
          [
              pending: new AtomicInteger(),
              ready: new LinkedBlockingQueue<R>(),

              scheduleIfNeeded: {->
                while (self && ready.size() + pending.get() < maxConcurrentTasks) {
                  pending.incrementAndGet()
                  def nextElement = self.next()
                  executor.execute {-> ready << op.call(nextElement); pending.decrementAndGet() }
                }
              },

              next: {->
                def res = ready.take()
                scheduleIfNeeded()
                res
              },

              hasNext: {-> scheduleIfNeeded(); pending.get() > 0 || !ready.empty },

              remove: {-> throw new UnsupportedOperationException("remove () is unsupported by the iterator") },
          ]
       }


        @Typed(value=TypePolicy.MIXED)
        void u () {
            new MarkupBuilder ().numbers {
                def divisors = { int n, Collection alreadyFound = [] ->
                    if (n > 3)
                        for(candidate in 2..<n)
                           if (n % candidate == 0)
                              return call (n / candidate, alreadyFound << candidate)

                    alreadyFound << n
                }

                (2..1500).iterator ().mapConcurrently (Executors.newFixedThreadPool(10), 50) {
                    [ it, divisors(it) ]
                }.each { pair ->
                    if (pair [1].size () == 1)
                        number ([value: pair[0], prime:true ])
                    else {
                        number ([value: pair[0], prime:false]) {
                          pair[1].each { div ->
                            divisor([value: div])
                          }
                        }
                    }
                }
            }
        }

        u ()
      """
  }

    void testConcurrentlyEMC () {
        shell.evaluate """
          import java.util.concurrent.*
          import java.util.concurrent.atomic.*
          import groovy.xml.*

          static <T,R> Iterator<R> mapConcurrently (Iterator<T> self, Executor executor, int maxConcurrentTasks, Function1<T,R> op) {
            [
                pending: new AtomicInteger(),
                ready: new LinkedBlockingQueue<R>(),

                scheduleIfNeeded: {->
                  while (self && ready.size() + pending.get() < maxConcurrentTasks) {
                    pending.incrementAndGet()
                    def nextElement = self.next()
                    executor.execute {-> ready << op(nextElement); pending.decrementAndGet() }
                  }
                },

                next: {->
                  def res = ready.take()
                  scheduleIfNeeded()
                  res
                },

                hasNext: {-> scheduleIfNeeded(); pending.get() > 0 || !ready.empty },

                remove: {-> throw new UnsupportedOperationException("remove () is unsupported by the iterator") },
            ]
         }


          @Typed(value=TypePolicy.MIXED)
          void u () {
              new MarkupBuilder ().numbers {
                  Integer.metaClass.getDivisors = { Collection alreadyFound = [] ->
                      int n = delegate
                      if (n > 3)
                          for(candidate in 2..<n)
                             if (n % candidate == 0)
                                return (n.intdiv(candidate)).getDivisors(alreadyFound << candidate)

                      alreadyFound << n
                  }

                  (2..1500).iterator ().mapConcurrently (Executors.newFixedThreadPool(10), 50) {
                      [ it, it.divisors ]
                  }.each { pair ->
                      if (pair [1].size () == 1)
                          number ([value: pair[0], prime:true ])
                      else {
                          number ([value: pair[0], prime:false]) {
                            pair[1].each { div ->
                              divisor([value: div])
                            }
                          }
                      }
                  }
              }
          }

          u ()
        """
    }
}