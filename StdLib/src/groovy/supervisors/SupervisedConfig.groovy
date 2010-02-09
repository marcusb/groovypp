package groovy.supervisors

@Typed class SupervisedConfig {
    SupervisedConfig parent

    DelegatingFunction0<Supervised,?> afterCreated
    DelegatingFunction0<Supervised,?> beforeStart
    DelegatingFunction0<Supervised,?> afterStart
    DelegatingFunction0<Supervised,?> beforeStop
    DelegatingFunction0<Supervised,?> afterStop
    DelegatingFunction0<Supervised,?> afterChildsCreated

    List<SupervisedConfig> childs

    Class<Supervised> klazz = Supervised

    Supervised create(Supervised parent = null) {
        def monitored = klazz.newInstance()
        monitored.config = this

        parent?.addChild(monitored)

        afterCreated?.call(monitored)

        childs?.each { childConfig ->
            childConfig.create(monitored)
        }

        afterChildsCreated?.call(monitored)

        monitored
    }
}
