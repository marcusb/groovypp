package primenumbers

import groovy.util.concurrent.FHashMap
import java.util.concurrent.atomic.AtomicReference
import groovy.xml.MarkupBuilder
import groovy.util.concurrent.FList
import java.util.concurrent.LinkedBlockingQueue

@Typed class PrimeCache {
    final AtomicReference<FHashMap<Integer,Boolean>> primes = [FHashMap.emptyMap.put(2,true)]

    FList<Integer> divisors(int n, FList<Integer> alreadyFound = FList.emptyList) {
        if (n > 2) {
            if(!primes[n]) {
                for (p in primes) {
                    int candidate = p.key
                    if (n % candidate == 0)
                        return divisors (n / candidate, alreadyFound + candidate)
                }

                for(candidate in 2..<n)
                   if (n % candidate == 0)
                        return divisors (n / candidate, alreadyFound + candidate)
            }
        }

        if (alreadyFound.empty)
            primes.apply { p -> p.put(n,true) }

        alreadyFound + n
    }
}

@Typed(value=TypePolicy.MIXED)
void generate () {

    def primeCache = new PrimeCache()

    def queue = new LinkedBlockingQueue<List>()

    def builderProcess = Thread.start {
        new MarkupBuilder().numbers {
            for (List pair; pair = queue.take();) {
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
        }
    }

    for(n in 2..1500) {
        queue << [n, primeCache.divisors(n)]
    }
    queue << []

    builderProcess.join ()

    def keys = primeCache.primes*.key.sort()
    println "\nNumber of primes: ${keys.size()}"
    for(k in keys) {
        println k
    }
}

generate ()