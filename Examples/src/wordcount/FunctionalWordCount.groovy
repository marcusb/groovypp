@Typed
package wordcount

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference
import groovy.util.concurrent.FHashMap
import java.util.concurrent.atomic.AtomicInteger

def t1 = System.currentTimeMillis()
AtomicReference<FHashMap<?,AtomicInteger>> counts = [FHashMap.emptyMap]
def pool = Executors.newFixedThreadPool(10)

new File("./20_newsgroups").recurseFileIterator().filter{ file ->
  !file.directory && !file.path.contains(".svn")
}.mapConcurrently(pool, false, 10) { file ->
    file.text.toLowerCase().eachMatch(/\w+/) { w ->
        counts.apply { c ->
            def wc = c[w]
            if (!wc)
                c.put(w, new AtomicInteger(1))
            else {
                wc.incrementAndGet ()
                c
            }
        }
    }
}.each {}


println "Calculated in ${System.currentTimeMillis() - t1} millis"

//pool.execute {
//    new File("counts-descreasing-groovy").withWriter { Writer out ->
//      counts.sort { a, b -> b.value <=> a.value }.each { k, v -> out << "$k\t$v\n" }
//    }
//}
//
//pool.execute {
//    new File("counts-alphabetical-groovy").withWriter { Writer out ->
//      counts.sort { a, b -> b.key <=> a.key }.each { k, v -> out << "$k\t$v\n" }
//    }
//}
pool.shutdown()
pool.awaitTermination(30,TimeUnit.SECONDS)

println "Finished in ${System.currentTimeMillis() - t1} millis"