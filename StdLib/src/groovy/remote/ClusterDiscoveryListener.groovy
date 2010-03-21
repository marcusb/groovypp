package groovy.remote

/**
 * Listener for discovery events
 */
@Typed abstract class ClusterDiscoveryListener {
    enum DiscoveryEventType {
        JOINED,
        LEFT,
        CRASHED
    }

    protected ClusterNode clusterNode

    abstract void onDiscoveryEvent (DiscoveryEventType eventType, RemoteClusterNode node)
}