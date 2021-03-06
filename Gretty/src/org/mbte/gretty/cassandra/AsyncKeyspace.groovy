package org.mbte.gretty.cassandra

import org.mbte.gretty.cassandra.thrift.*

import java.nio.ByteBuffer
import groovy.util.concurrent.BindLater

@Typed class AsyncKeyspace {

    final AsyncCassandra resource
    final String keyspace

    AsyncKeyspace(AsyncCassandra resource, String name) {
        this.resource = resource
        this.keyspace = name
    }

    AsyncColumnPath getColumnPath(String column_family, String column) {
        [this, column_family, column]
    }

    BindLater<Void> login(AuthenticationRequest auth_request, BindLater.Listener<Void> listener = null) {
        resource.login(keyspace, auth_request, listener)
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
    BindLater<ColumnOrSuperColumn> get(String key, ColumnPath column_path, ConsistencyLevel consistency_level, BindLater.Listener<ColumnOrSuperColumn> listener = null) {
        resource.get(keyspace, key, column_path, consistency_level, listener)
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
    BindLater<List<ColumnOrSuperColumn>> get_slice(String key, ColumnParent column_parent, SlicePredicate predicate, ConsistencyLevel consistency_level, BindLater.Listener<List<ColumnOrSuperColumn>> listener = null) {
        resource.get_slice(keyspace, key, column_parent, predicate, consistency_level, listener)
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
    BindLater<Map<String,ColumnOrSuperColumn>> multiget(List<String> keys, ColumnPath column_path, ConsistencyLevel consistency_level, BindLater.Listener<BindLater<Map<String,ColumnOrSuperColumn>>> listener = null) {
        resource.multiget(keyspace, keys, column_path, consistency_level, listener)
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
    BindLater<Map<String,List<ColumnOrSuperColumn>>> multiget_slice(List<String> keys, ColumnParent column_parent, SlicePredicate predicate, ConsistencyLevel consistency_level, BindLater.Listener<Map<String,List<ColumnOrSuperColumn>>> listener = null) {
        resource.multiget_slice(keyspace, keys, column_parent, predicate, consistency_level, listener)
    }

    /**
     * returns the number of columns for a particular <code>key</code> and <code>ColumnFamily</code> or <code>SuperColumn</code>.
     *
     * @param keyspace
     * @param key
     * @param column_parent
     * @param consistency_level
     */
    BindLater<Integer> get_count(String key, ColumnParent column_parent, ConsistencyLevel consistency_level, BindLater.Listener<Integer> listener = null) {
        resource.get_count(keyspace, key, column_parent, consistency_level, listener)
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
    BindLater<List<KeySlice>> get_range_slice(ColumnParent column_parent, SlicePredicate predicate, String start_key, String finish_key, int row_count, ConsistencyLevel consistency_level, BindLater.Listener<List<KeySlice>> listener = null) {
        resource.get_range_slice(keyspace, column_parent, predicate, start_key, finish_key, row_count, consistency_level, listener)
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
    BindLater<List<KeySlice>> get_range_slices(ColumnParent column_parent, SlicePredicate predicate, KeyRange range, ConsistencyLevel consistency_level, BindLater.Listener<List<KeySlice>> listener = null){
        resource.get_range_slices(keyspace, column_parent, predicate, range, consistency_level, listener)
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
    BindLater<Void> insert(String key, ColumnPath column_path, ByteBuffer value, long timestamp, ConsistencyLevel consistency_level, BindLater.Listener<Void> listener = null)  {
        resource.insert(keyspace, key, column_path, value, timestamp, consistency_level, listener)
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
    BindLater<Void> batch_insert(String key, Map<String,List<ColumnOrSuperColumn>> cfmap, ConsistencyLevel consistency_level, BindLater.Listener<Void> listener = null) {
        resource.batch_insert(keyspace, key, cfmap, consistency_level, listener)
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
    BindLater<Void> remove(String key, ColumnPath column_path, long timestamp, ConsistencyLevel consistency_level, BindLater.Listener<Void> listener = null) {
        resource.remove(keyspace, key, column_path, timestamp, consistency_level, listener)
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
    BindLater<Void> batch_mutate(Map<String,Map<String,List<Mutation>>> mutation_map, ConsistencyLevel consistency_level, BindLater.Listener<Void> listener = null) {
        resource.batch_mutate(keyspace, mutation_map, consistency_level, listener)
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
    BindLater<List<TokenRange>> describe_ring(BindLater.Listener<List<TokenRange>> listener = null) {
        resource.describe_ring(keyspace, listener)
    }

    /**
     * describe specified keyspace
     *
     * @param keyspace
     */
    BindLater<Map<String,Map<String,String>>> describe_keyspace(BindLater.Listener<Map<String,Map<String,String>>> listener = null) {
        resource.describe_keyspace(keyspace, listener)
    }

    boolean isConnected () {
        resource.isConnected()
    }
}

