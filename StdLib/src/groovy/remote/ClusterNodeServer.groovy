package groovy.remote

import groovy.supervisors.Supervised
import groovy.supervisors.SupervisedConfig
import groovy.util.concurrent.SupervisedChannel

@Typed abstract class ClusterNodeServer extends SupervisedChannel {
    ClusterNode getClusterNode () {
        owner
    }
}
