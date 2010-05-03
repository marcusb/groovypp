@Typed package actors

import groovy.util.concurrent.Agent
import groovy.util.concurrent.FVector
import java.util.concurrent.CountDownLatch

testWithFixedPool(20) {
    Agent<FVector<Integer>> sharedVector = [FVector.emptyVector]
    sharedVector.executor = pool

    CountDownLatch initCdl = [1]

    def validator = sharedVector.addValidator { agent, newValue ->
        newValue.length <= 100*100 
    }

    def listener = sharedVector.addListener { agent ->
        def vec = agent.get()
        println "${vec.length} ${vec[-1]}"
        if(vec.length == 100*100) {
            initCdl.countDown()
        }
    }

    for(i in 0..<100) {
        Mutation<FVector<Integer>> action = {
            it.length < 100*100 ? it + it.length : it 
        }
        sharedVector(action) {
            if (sharedVector.get().length < 100*100)
                sharedVector(action, this)
        }
    }
    initCdl.await()
    sharedVector.removeValidator(validator)
    sharedVector.removeListener(listener)

    assert sharedVector.get().length == 100*100
    assert sharedVector.get().asList() == (0..<100*100)

    CountDownLatch shuffleCdl = [10]
    for(i in 0..<10) {
        pool.execute {
            def r = new Random ()
            for(j in 0..<1000) {
                def i1 = r.nextInt (100*100)
                def i2 = r.nextInt (100*100)
                sharedVector.await {
                    def v1 = it[i1]
                    def v2 = it[i2]
                    it.set(i2, v1).set(i1, v2)
                }
            }
            shuffleCdl.countDown()
        }
    }
    shuffleCdl.await ()
    assert sharedVector.get().asList().sort() == (0..<100*100)
}
