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

import org.apache.cassandra.thrift.ColumnPath
import org.apache.cassandra.thrift.ConsistencyLevel

import org.apache.thrift.protocol.TBinaryProtocol

import org.apache.thrift.transport.TSocket

import java.util.concurrent.CountDownLatch
import groovy.util.concurrent.ResourcePool
import groovy.util.concurrent.FList

def UTF8 = "UTF8"

def keyspace = "Keyspace1"
def columnFamily = "Standard1"

def colPathName = new ColumnPath(column_family:columnFamily, column:"fullName".getBytes(UTF8))
def colPathAge  = new ColumnPath(column_family:columnFamily, column:"age".getBytes(UTF8))

testWithFixedPool(10) {
    ResourcePool<Cassandra.Client> cassandraPool = [
    executor: pool,
    initResources: {
        FList<Cassandra.Client> cp = FList.emptyList
        for (i in 0..<3) {
            def transport = new TSocket("localhost", 9160)
            def client = new Cassandra.Client(new TBinaryProtocol(transport))
            transport.open()
            cp = cp + client
        }
        cp
    }]

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
        }{
            cdl.countDown()
        }
    }

    cdl.await()
    def elapsed = System.currentTimeMillis() - totalStart
    println "${elapsed}ms ${1.0d*elapsed/(20*integers.size())}"
}
