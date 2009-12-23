package primenumbers

import groovy.util.concurrent.FHashMap
import java.util.concurrent.atomic.AtomicReference
import groovy.xml.MarkupBuilder
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService

@Typed List<Integer> divisors(int n, Collection<Integer> alreadyFound = []) {
    if (n > 3)
        for(candidate in 2..<n)
           if (n % candidate == 0)
                return divisors (n / candidate, alreadyFound << candidate)

    alreadyFound << n
}

@Typed(value=TypePolicy.MIXED)
void generate () {
    AtomicReference<FHashMap<Integer,Boolean>> primes = [FHashMap.emptyMap]

    new MarkupBuilder ().numbers {
        ExecutorService pool = Executors.newFixedThreadPool(10)
        (2..1500).iterator ().mapConcurrently (pool, 50) {
            List divs = divisors(it)
            if (divs.size() == 1)
                primes.apply { p -> p.put(it, true) }
            [ it, divs ]
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
        pool.shutdown()
    }

    println "\nNumber of primes: ${primes.get().size()}"
    def keys = primes.get().iterator().map {
        it.key
    }.asList()
    assert primes.get().size() == keys.size ()
    keys.sort ()
    for(k in keys) {
        println "${k}"
    }
}

generate ()