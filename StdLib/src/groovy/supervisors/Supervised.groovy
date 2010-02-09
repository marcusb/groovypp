package groovy.supervisors

@Typed class Supervised {
    Supervised       parent
    SupervisedConfig config

    List<Supervised> childs

    void start () {
        config.beforeStart?.call(this)

        childs?.each { child ->
            child.start ()
        }

        config.afterStart?.call(this)
    }

    void stop () {
        config.beforeStop?.call(this)

        childs?.reverse()?.each { child ->
            child.stop ()
        }

        config.afterStop?.call(this)
    }

    void addChild(Supervised child) {
        child.parent = this
        if (childs == null) {
            childs = []
        }
        childs << child
    }
}
