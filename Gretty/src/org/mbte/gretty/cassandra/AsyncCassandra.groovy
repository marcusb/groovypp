package org.mbte.gretty.cassandra

import org.mbte.gretty.cassandra.thrift.*

import java.nio.ByteBuffer
import groovy.util.concurrent.BindLater
import java.util.concurrent.Executors
import groovy.util.concurrent.ResourcePool
import org.apache.thrift.transport.TSocket
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TFramedTransport
import org.apache.thrift.transport.TTransportException
import groovy.util.concurrent.BindLater.Listener

@Typed class AsyncCassandra extends ResourcePool<Cassandra.Client> {

    AsyncCassandra(List<String> seedHosts = Collections.emptyList()) {
        executor = Executors.newFixedThreadPool(10)
        for(host in seedHosts) {
            executor.execute {
                discover(host, 9160)
            }
        }
    }

    AsyncCassandra(String seedHost) {
            this([seedHost])
    }

    Iterable<Cassandra.Client> initResources () { [] }

    BindLater<Void> login(String keyspace, AuthenticationRequest auth_request, BindLater.Listener<Void> listener = null) {
        execute({ resource ->
            resource.login(keyspace, auth_request)
        }, listener)
    }

    /**
     * Get the Column or SuperColumn at the given column_path. If no value is present, NotFoundException is thrown. (This is
     * the only method that can throw an exception under non-failure conditions.)
     *
     * @param keyspace
     * @param key
     * @param column_path
     * @param consistency_level
     */
    BindLater<ColumnOrSuperColumn> get(String keyspace, String key, ColumnPath column_path, ConsistencyLevel consistency_level, BindLater.Listener<ColumnOrSuperColumn> listener = null) {
        execute({ resource ->
            resource.get(keyspace, key, column_path, consistency_level)
        }, listener)
    }

    /**
     * Get the group of columns contained by column_parent (either a ColumnFamily name or a ColumnFamily/SuperColumn name
     * pair) specified by the given SlicePredicate. If no matching values are found, an empty list is returned.
     *
     * @param keyspace
     * @param key
     * @param column_parent
     * @param predicate
     * @param consistency_level
     */
    BindLater<List<ColumnOrSuperColumn>> get_slice(String keyspace, String key, ColumnParent column_parent, SlicePredicate predicate, ConsistencyLevel consistency_level, BindLater.Listener<List<ColumnOrSuperColumn>> listener = null) {
        execute({ resource ->
            resource.get_slice(keyspace, key, column_parent, predicate, consistency_level)
        }, listener)
    }

    /**
     * Perform a get for column_path in parallel on the given list<string> keys. The return value maps keys to the
     * ColumnOrSuperColumn found. If no value corresponding to a key is present, the key will still be in the map, but both
     * the column and super_column references of the ColumnOrSuperColumn object it maps to will be null.
     * @deprecated; use multiget_slice
     *
     * @param keyspace
     * @param keys
     * @param column_path
     * @param consistency_level
     */
    BindLater<Map<String,ColumnOrSuperColumn>> multiget(String keyspace, List<String> keys, ColumnPath column_path, ConsistencyLevel consistency_level, BindLater.Listener<BindLater<Map<String,ColumnOrSuperColumn>>> listener = null) {
        execute({ resource ->
            resource.multiget(keyspace, keys, column_path, consistency_level)
        }, listener)
    }

    /**
     * Performs a get_slice for column_parent and predicate for the given keys in parallel.
     *
     * @param keyspace
     * @param keys
     * @param column_parent
     * @param predicate
     * @param consistency_level
     */
    BindLater<Map<String,List<ColumnOrSuperColumn>>> multiget_slice(String keyspace, List<String> keys, ColumnParent column_parent, SlicePredicate predicate, ConsistencyLevel consistency_level, BindLater.Listener<Map<String,List<ColumnOrSuperColumn>>> listener = null) {
        execute({ resource ->
            resource.multiget_slice(keyspace, keys, column_parent, predicate, consistency_level)
        }, listener)
    }

    /**
     * returns the number of columns for a particular <code>key</code> and <code>ColumnFamily</code> or <code>SuperColumn</code>.
     *
     * @param keyspace
     * @param key
     * @param column_parent
     * @param consistency_level
     */
    BindLater<Integer> get_count(String keyspace, String key, ColumnParent column_parent, ConsistencyLevel consistency_level, BindLater.Listener<Integer> listener = null) {
        execute({ resource ->
            resource.get_count(keyspace, key, column_parent, consistency_level)
        }, listener)
    }

    /**
     * returns a subset of columns for a range of keys.
     * @Deprecated.  Use get_range_slices instead
     *
     * @param keyspace
     * @param column_parent
     * @param predicate
     * @param start_key
     * @param finish_key
     * @param row_count
     * @param consistency_level
     */
    BindLater<List<KeySlice>> get_range_slice(String keyspace, ColumnParent column_parent, SlicePredicate predicate, String start_key, String finish_key, int row_count, ConsistencyLevel consistency_level, BindLater.Listener<List<KeySlice>> listener = null) {
        execute({ resource ->
            resource.get_range_slice(keyspace, column_parent, predicate, start_key, finish_key, row_count, consistency_level)
        }, listener)
    }

    /**
     * returns a subset of columns for a range of keys.
     *
     * @param keyspace
     * @param column_parent
     * @param predicate
     * @param range
     * @param consistency_level
     */
    BindLater<List<KeySlice>> get_range_slices(String keyspace, ColumnParent column_parent, SlicePredicate predicate, KeyRange range, ConsistencyLevel consistency_level, BindLater.Listener<List<KeySlice>> listener = null){
        execute({ resource ->
            resource.get_range_slices(keyspace, column_parent, predicate, range, consistency_level)
        }, listener)
    }

    /**
     * Insert a Column consisting of (column_path.column, value, timestamp) at the given column_path.column_family and optional
     * column_path.super_column. Note that column_path.column is here required, since a SuperColumn cannot directly contain binary
     * values -- it can only contain sub-Columns.
     *
     * @param keyspace
     * @param key
     * @param column_path
     * @param value
     * @param timestamp
     * @param consistency_level
     */
    BindLater<Void> insert(String keyspace, String key, ColumnPath column_path, ByteBuffer value, long timestamp, ConsistencyLevel consistency_level, BindLater.Listener<Void> listener = null)  {
        execute({ resource ->
            resource.insert(keyspace, key, column_path, value, timestamp, consistency_level)
        }, listener)
    }

    /**
     * Insert Columns or SuperColumns across different Column Families for the same row key. batch_mutation is a
     * map<string, list<ColumnOrSuperColumn>> -- a map which pairs column family names with the relevant ColumnOrSuperColumn
     * objects to insert.
     * @deprecated; use batch_mutate instead
     *
     * @param keyspace
     * @param key
     * @param cfmap
     * @param consistency_level
     */
    BindLater<Void> batch_insert(String keyspace, String key, Map<String,List<ColumnOrSuperColumn>> cfmap, ConsistencyLevel consistency_level, BindLater.Listener<Void> listener = null) {
        execute({ resource ->
            resource.batch_insert(keyspace, key, cfmap,consistency_level)
        }, listener)
    }

    /**
     * Remove data from the row specified by key at the granularity specified by column_path, and the given timestamp. Note
     * that all the values in column_path besides column_path.column_family are truly optional: you can remove the entire
     * row by just specifying the ColumnFamily, or you can remove a SuperColumn or a single Column by specifying those levels too.
     *
     * @param keyspace
     * @param key
     * @param column_path
     * @param timestamp
     * @param consistency_level
     */
    BindLater<Void> remove(String keyspace, String key, ColumnPath column_path, long timestamp, ConsistencyLevel consistency_level, BindLater.Listener<Void> listener = null) {
        execute({ resource ->
            resource.remove(keyspace, key, column_path, timestamp, consistency_level)
        }, listener)
    }

    /**
     *   Mutate many columns or super columns for many row keys. See also: Mutation.
     *
     *   mutation_map maps key to column family to a list of Mutation objects to take place at that scope.
     * *
     *
     * @param keyspace
     * @param mutation_map
     * @param consistency_level
     */
    BindLater<Void> batch_mutate(String keyspace, Map<String,Map<String,List<Mutation>>> mutation_map, ConsistencyLevel consistency_level, BindLater.Listener<Void> listener = null) {
        execute({ resource ->
            resource.batch_mutate(keyspace, mutation_map, consistency_level)
        }, listener)
    }

    /**
     * get property whose value is of type string. @Deprecated
     *
     * @param property
     */
    BindLater<String> get_string_property(String property, BindLater.Listener<String> listener = null) {
        execute({ resource ->
            resource.get_string_property(property)
        }, listener)
    }

    /**
     * get property whose value is list of strings. @Deprecated
     *
     * @param property
     */
    BindLater<List<String>> get_string_list_property(String property, BindLater.Listener<List<String>> listener = null) {
        execute({ resource ->
            resource.get_string_list_property(property)
        }, listener)
    }

    /**
     * list the defined keyspaces in this cluster
     */
    BindLater<Set<String>> describe_keyspaces(BindLater.Listener<Set<String>> listener = null) {
        execute({ resource ->
            resource.describe_keyspaces()
        }, listener)
    }

    /**
     * get the cluster name
     */
    BindLater<String> describe_cluster_name(BindLater.Listener<String> listener = null) {
        execute({ resource ->
            resource.describe_cluster_name()
        }, listener)
    }

    /**
     * get the thrift api version
     */
    BindLater<String> describe_version(BindLater.Listener<String> listener = null) {
        execute({ resource ->
            resource.describe_version()
        }, listener)
    }

    /**
     * get the token ring: a map of ranges to host addresses,
     * represented as a set of TokenRange instead of a map from range
     * to list of endpoints, because you can't use Thrift structs as
     * map keys:
     * https://issues.apache.org/jira/browse/THRIFT-162
     *
     * for the same reason, we can't return a set here, even though
     * order is neither important nor predictable.
     *
     * @param keyspace
     */
    BindLater<List<TokenRange>> describe_ring(String keyspace, BindLater.Listener<List<TokenRange>> listener = null) {
        execute({ resource ->
            resource.describe_ring(keyspace)
        }, listener)
    }

    /**
     * returns the partitioner used by this cluster
     */
    BindLater<String> describe_partitioner(BindLater.Listener<String> listener = null) {
        execute({ resource ->
            resource.describe_partitioner()
        }, listener)
    }

    /**
     * describe specified keyspace
     *
     * @param keyspace
     */
    BindLater<Map<String,Map<String,String>>> describe_keyspace(String keyspace, BindLater.Listener<Map<String,Map<String,String>>> listener = null) {
        execute({ resource ->
            resource.describe_keyspace(keyspace)
        }, listener)
    }

    /**
     * experimental API for hadoop/parallel query support.
     * may change violently and without warning.
     *
     * returns list of token strings such that first subrange is (list[0], list[1]],
     * next is (list[1], list[2]], etc.
     *
     * @param start_token
     * @param end_token
     * @param keys_per_split
     */
    BindLater<List<String>> describe_splits(String start_token, String end_token, int keys_per_split, BindLater.Listener<List<String>> listener = null) {
    }

    boolean isConnected () {
        synchronized(connected) {
            !connected.empty
        }
    }

    private final HashSet<HostPort> connected = []

    protected void discover(String host, int port) {
        InetAddress ip
        try {
            ip = InetAddress.getByName(host)
        }
        catch(UnknownHostException e) {
            return
        }

        synchronized(connected) {
            HostPort hp = [host:ip, port:port]

            if (!connected.contains(hp)) {
                try {
                    TSocket transport = [host, port]
                    transport.open ()

                    Cassandra.Client cassandra = [new TBinaryProtocol(new TFramedTransport(transport))]
                    add(cassandra)
                    connected << hp
                }
                catch(IOException e)  { //
                }
                catch(TTransportException e)  { //
                }
            }
        }
    }

    private static class HostPort {
        InetAddress host
        int    port

        boolean equals(o) {
            if (this.is(o)) return true;

            if (getClass() != o.class) return false;

            HostPort hostPort = o
            return port == hostPort.port && host == hostPort.port
        }

        int hashCode() {
            int result;

            result = (host != null ? host.hashCode() : 0);
            result = 31 * result + port;
            return result;
        }
    }
}

