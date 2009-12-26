package primenumbers

import groovy.util.concurrent.FHashMap
import java.util.concurrent.atomic.AtomicReference
import groovy.xml.MarkupBuilder
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import groovy.util.concurrent.FList
import groovy.util.concurrent.Agent

@Typed FList<Integer> divisors(int n, FList<Integer> alreadyFound = FList.emptyList) {
    if (n > 3)
        for(candidate in 2..<n)
           if (n % candidate == 0)
                return divisors (n / candidate, alreadyFound + candidate)

    alreadyFound + n
}

@Typed(value=TypePolicy.MIXED)
void generate () {
    AtomicReference<FHashMap<Integer,Boolean>> primes = [FHashMap.emptyMap]

    new MarkupBuilder().numbers {
        ExecutorService pool = Executors.newFixedThreadPool(10)

        (2..1500).iterator ().mapConcurrently (pool, 50) {
            def divs = divisors(it)

            if (divs.size() == 1)
                primes.apply { p -> p.put(it, true) }

            [it, divs]
        }.each { pair ->
            if (pair [1].size () == 1)
                number (value: pair[0], prime:true)
            else {
                number (value: pair[0], prime:false) {
                  pair[1].reverse().each { div ->
                    divisor(value: div)
                  }
                }
            }
        }

        pool.shutdown()
    }

    def keys = primes.get()*.key.sort()
    println "\nNumber of primes: ${keys.size()}"
    for(k in keys) {
        println k
    }
}

generate ()