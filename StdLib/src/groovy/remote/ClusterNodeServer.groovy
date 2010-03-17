package groovy.remote

import groovy.supervisors.Supervised
import groovy.supervisors.SupervisedConfig

@Typed abstract class ClusterNodeServer {
    ClusterNode clusterNode

    abstract void start ()

    abstract void stop ()
}
