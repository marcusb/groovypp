package groovy.remote

import groovy.supervisors.Supervised
import groovy.supervisors.SupervisedConfig

class ClusterNodeServer<C extends ClusterNodeServer.Config> extends Supervised<C> {

    ClusterNode clusterNode

    @Trait abstract static class Config<S extends ClusterNodeServer>
        implements
            SupervisedConfig,
            RemoteConnection.Config {
    }
}
