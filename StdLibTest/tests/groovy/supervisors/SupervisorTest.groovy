package groovy.supervisors

class SupervisorTest {
    void testMe () {
        def config = SupervisionConfig.configure {
            beforeStart {
                println "supervisor starting"
            }

            afterStop {
                println "supervisor stopped"
            }

            worker {
                afterStart {
                    println "worker started"
                }

                beforeStop {
                    println "worker stopped"
                }
            }
        }
//        Supervised supervisor = config.create()
        supervisor.start ()
        supervisor.stop ()
    }
}