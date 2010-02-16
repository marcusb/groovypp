package groovy.remote

import groovy.supervisors.Supervised

class ClusterClient {

    @Trait abstract static class Config implements RemoteConnection.Config {
    }
}
