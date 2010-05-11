/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@Typed package org.mbte.groovypp.samples.zookeeper

import org.apache.cassandra.thrift.Cassandra

import org.apache.cassandra.thrift.ColumnOrSuperColumn
import org.apache.cassandra.thrift.ColumnParent
import org.apache.cassandra.thrift.ColumnPath
import org.apache.cassandra.thrift.ConsistencyLevel
import org.apache.cassandra.thrift.InvalidRequestException
import org.apache.cassandra.thrift.NotFoundException
import org.apache.cassandra.thrift.SlicePredicate

import org.apache.cassandra.thrift.TimedOutException
import org.apache.cassandra.thrift.UnavailableException
import org.apache.thrift.TException
import org.apache.thrift.protocol.TBinaryProtocol

import org.apache.thrift.transport.TSocket
import groovy.util.concurrent.CallLaterExecutors
import java.util.concurrent.CountDownLatch
import groovy.channels.ResourcePool
import groovy.util.concurrent.FList


public class CassandraTest {

        public static final String UTF8 = "UTF8"

        public static void main(String[] args) throws UnsupportedEncodingException,
                        InvalidRequestException, UnavailableException, TimedOutException,
                        TException, NotFoundException {

                def keyspace = "Keyspace1"
                def columnFamily = "Standard1"

                def colPathName = new ColumnPath(column_family:columnFamily, column:"fullName".getBytes(UTF8))
                def colPathAge  = new ColumnPath(column_family:columnFamily, column:"age".getBytes(UTF8))

                testWithFixedPool(10) {
                    ResourcePool<Cassandra.Client> cassandraPool = {
                        FList<Cassandra.Client> cp = FList.emptyList
                        for (i in 0..<3) {
                            def transport = new TSocket("localhost", 9160)
                            def client = new Cassandra.Client(new TBinaryProtocol(transport))
                            transport.open()
                            cp = cp + client
                        }
                        cp
                    }
                    cassandraPool.executor = pool

                    def integers = 0..<1000
                    CountDownLatch cdl = [20]
                    def totalStart = System.currentTimeMillis()
                    for (j in 0..<20) {
                        cassandraPool.execute { client ->
                            def start = System.currentTimeMillis()
                            for (i in integers) {
//                            def timestamp = System.currentTimeMillis()
                                client.insert(keyspace, i, colPathName, "Chris Goffinet$i".toString().getBytes(UTF8), start, ConsistencyLevel.ONE)
                                client.insert(keyspace, i, colPathAge, "$j".toString().getBytes(UTF8), start, ConsistencyLevel.ONE)
                            }
                            def elapsed = System.currentTimeMillis() - start
                            println "Thread$j: ${integers.size()} inserts in $elapsed ms ${(1.0d*elapsed)/integers.size()}"

                            cdl.countDown()
                        }
                    }

                    cdl.await()
                    def elapsed = System.currentTimeMillis() - totalStart
                    println "${elapsed}ms ${1.0d*elapsed/(20*integers.size())}"
                }

//
//                // read single column
//                println("single column:")
//                def col = client.get(keyspace, "1", colPathName, ConsistencyLevel.ONE).column
//
//                println("column name: " + new String(col.name, UTF8))
//                println("column value: " + new String(col.value, UTF8))
//                println("column timestamp: " + new Date(col.timestamp))
//
//                // read entire row
//                def allColumns = new SlicePredicate(slice_range:[start:new byte[0], finish:new byte[0]])
//
//                println("\nrow:")
//                def parent = new ColumnParent(columnFamily)
//                def results = client.get_slice(keyspace, "1", parent, allColumns, ConsistencyLevel.ONE)
//                for (result in results) {
//                    def column = result.column
//                    println(new String(column.name, UTF8) + " -> " + new String(column.value, UTF8))
//                }
        }
}
