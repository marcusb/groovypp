package org.mbte.gretty.cassandra

import org.apache.thrift.async.TAsyncClientManager
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TNonblockingSocket
import org.mbte.gretty.cassandra.thrift.Cassandra
import org.apache.thrift.transport.TFramedTransport
import org.apache.thrift.transport.TSocket

@Typed abstract class CassandraTestBase extends GroovyTestCase {
    AsyncCassandra cassandra = []

    protected void setUp() {
        super.setUp()
        cassandra.discover("localhost", 9160)

        if(cassandra.connected) {
            cassandra.describe_keyspaces{ bl ->
                for(ks in bl.get()) {
                    cassandra.describe_ring(ks) { br ->
                        if(!br.isException())
                            println br.get ()
                    }
                }
            }
        }
    }

    protected void tearDown() {
        super.tearDown()
    }
}
