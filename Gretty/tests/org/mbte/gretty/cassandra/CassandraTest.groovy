package org.mbte.gretty.cassandra

import org.mbte.gretty.cassandra.thrift.ColumnPath
import org.mbte.gretty.cassandra.thrift.ConsistencyLevel
import org.mbte.gretty.cassandra.thrift.Column
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Typed class CassandraTest extends CassandraTestBase{
    public static final String UTF8 = "UTF8";

    void testInsert () {
        if(cassandra.connected) {
            AsyncKeyspace keyspace = [cassandra, "Keyspace1"]
            String columnFamily = "Standard1";

            // insert data

            def colPathName = new AsyncColumnPath(keyspace, columnFamily, "fullName")
            def colPathAge  = new AsyncColumnPath(keyspace, columnFamily, "age")

            long start = System.currentTimeMillis();
            int nTrys = 1000;
            CountDownLatch cdl = [1000]
            Map<String, String> userids = new HashMap<String, String>();
            try {
                for (int i = 0; i < nTrys; ++i) {
                    if ((i % 10) == 0) System.out.println("try " + i);
                    double userid = Math.random() * nTrys * 10;
                    String keyUserID = String.valueOf(userid);
                    long timestamp = System.currentTimeMillis();

                    String name = "user " + (int)(Math.random()*100000);
                    userids[keyUserID] = name
                    colPathName.insert(keyUserID, ByteBuffer.wrap(name.getBytes(UTF8)), timestamp, ConsistencyLevel.DCQUORUM) {
                        cdl.countDown()
                    }

                    colPathAge.insert(keyUserID, ByteBuffer.wrap("24".getBytes(UTF8)), timestamp, ConsistencyLevel.DCQUORUM).get()
                }

                int i = 0;
                for (Map.Entry<String, String> entry : userids.entrySet()) {
                    if ((i % 10) == 0) System.out.println("read try " + i)
                    Column col = colPathName.get(entry.getKey(), ConsistencyLevel.ONE).get().getColumn()
                    def res = new String(col.value.array(), col.value.position(), col.value.remaining(), UTF8)
                    assert res.equals(entry.getValue())
                    i++
                }

                assert cdl.await(10,TimeUnit.SECONDS)
            } finally {
                long finish = System.currentTimeMillis();
                System.out.println("Elapsed ms " + (finish - start));
            }
        }
    }
}
