package groovy.supervisors

@Trait abstract class SupervisedConfig {
    int numberOfInstances = 1

    List<SupervisedConfig> childs

    Class<Supervised> klazz = Supervised

    DelegatingFunction0<Supervised,?> afterCreated
    DelegatingFunction0<Supervised,?> beforeStart
    DelegatingFunction0<Supervised,?> afterStart
    DelegatingFunction0<Supervised,?> beforeStop
    DelegatingFunction0<Supervised,?> afterStop
    DelegatingFunction0<Supervised,?> afterChildsCreated
    DelegatingFunction1<Supervised,Throwable,?> afterCrashed

    Supervised create(Supervised parent) {
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

    void setChilds (List<SupervisedConfig> childs) {
        if (this.childs == null)
            this.childs = childs
        else
            this.childs.addAll(childs)
    }

    Supervised createSupervised() {
        klazz.newInstance()
    }
}
