package groovy.supervisors

@Typed class SupervisedConfig {

    SupervisedConfig parent

    int numberOfInstances = 1

    DelegatingFunction0<Supervised,?> afterCreated
    DelegatingFunction0<Supervised,?> beforeStart
    DelegatingFunction0<Supervised,?> afterStart
    DelegatingFunction0<Supervised,?> beforeStop
    DelegatingFunction0<Supervised,?> afterStop
    DelegatingFunction0<Supervised,?> afterChildsCreated
    DelegatingFunction2<Supervised,String, Throwable,?> afterCrashed

    List<SupervisedConfig> childs

    Class<Supervised> klazz = Supervised

    Supervised create(Supervised parent = null) {
        def monitored = createSupervised()
        monitored.config = this

        parent?.addChild(monitored)

        afterCreated?.call(monitored)

        childs?.each { childConfig ->
            for ( i in 0..<childConfig.numberOfInstances)
                childConfig.create(monitored)
        }

        afterChildsCreated?.call(monitored)

        monitored
    }

    protected Supervised createSupervised() {
        klazz.newInstance()
    }
}
