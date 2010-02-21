package groovy.supervisors

@Trait abstract class SupervisedConfig {
    int numberOfInstances = 1

    List<SupervisedConfig> children

    Class<Supervised> klazz = Supervised

    DelegatingFunction0<Supervised,?> afterCreated
    DelegatingFunction0<Supervised,?> beforeStart
    DelegatingFunction0<Supervised,?> afterStart
    DelegatingFunction0<Supervised,?> beforeStop
    DelegatingFunction0<Supervised,?> afterStop
    DelegatingFunction0<Supervised,?> afterChildrenCreated
    DelegatingFunction1<Supervised,Throwable,?> afterCrashed

    Supervised create(Supervised parent) {
        def monitored = createSupervised()
        monitored.config = this

        parent?.addChild(monitored)

        afterCreated?.call(monitored)

        children?.each { childConfig ->
            for ( i in 0..<childConfig.numberOfInstances)
                childConfig.create(monitored)
        }

        afterChildrenCreated?.call(monitored)

        monitored
    }

    void setChildren (List<SupervisedConfig> children) {
        if (this.children == null)
            this.children = children
        else
            this.children.addAll(children)
    }

    Supervised createSupervised() {
        klazz.newInstance()
    }
}
