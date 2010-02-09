package groovy.supervisors

@Typed class SupervisorTest extends GroovyTestCase {
    void testMe () {
        SupervisedConfig config = [
            beforeStart: { println "supervisor starting" },

            afterStop: { println "supervisor stopped" },

            afterChildsCreated: {
                childs.each { c ->
                    println c
                }
            },

            childs: [
                [
                    afterStart: { println "worker started" },

                    beforeStop: { println "worker stopped" }
                ],

                [ klazz : Supervised ],
            ]
        ]

        Supervised supervisor = config.create()
        supervisor.start ()
        supervisor.stop ()
    }

    void testServer () {
        SupervisedConfig config = [
        ]

        Supervised supervisor = config.create()
        supervisor.start ()
        supervisor.stop ()
    }
}