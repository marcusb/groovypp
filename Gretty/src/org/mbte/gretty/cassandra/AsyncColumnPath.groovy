package org.mbte.gretty.cassandra

import groovy.util.concurrent.BindLater
import java.nio.ByteBuffer
import org.mbte.gretty.cassandra.thrift.*

@Typed class AsyncColumnPath {

    final AsyncKeyspace resource
    final ColumnPath column_path

    AsyncColumnPath(AsyncKeyspace resource, ColumnPath column_path) {
        this.resource = resource
        this.column_path = column_path
    }

    AsyncColumnPath(AsyncKeyspace resource, String column_family, String column) {
        this(resource, new ColumnPath(column_family).setColumn(ByteBuffer.wrap(column.getBytes("UTF-8"))))
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
    BindLater<ColumnOrSuperColumn> get(String key, ConsistencyLevel consistency_level, BindLater.Listener<ColumnOrSuperColumn> listener = null) {
        resource.get(key, column_path, consistency_level, listener)
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
    BindLater<Map<String,ColumnOrSuperColumn>> multiget(List<String> keys, ConsistencyLevel consistency_level, BindLater.Listener<BindLater<Map<String,ColumnOrSuperColumn>>> listener = null) {
        resource.multiget(keys, column_path, consistency_level, listener)
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
    BindLater<Void> insert(String key, ByteBuffer value, long timestamp, ConsistencyLevel consistency_level, BindLater.Listener<Void> listener = null)  {
        resource.insert(key, column_path, value, timestamp, consistency_level, listener)
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
    BindLater<Void> remove(String key, long timestamp, ConsistencyLevel consistency_level, BindLater.Listener<Void> listener = null) {
        resource.remove(key, column_path, timestamp, consistency_level, listener)
    }

    boolean isConnected () {
        resource.isConnected()
    }
}
