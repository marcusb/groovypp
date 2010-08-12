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

package org.mbte.gretty.redis

import groovy.util.concurrent.BindLater
import groovy.util.concurrent.FQueue
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.ChannelFactory
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.MessageEvent
import org.mbte.gretty.AbstractClient

import static org.mbte.gretty.redis.RedisCommand.MORE

@Typed class RedisClient extends AbstractClient {

    private static class State {
        // first element is waiting to read
        FQueue<RedisCommand>  readQueue = FQueue.emptyQueue

        // first element is waiting to be written
        FQueue<RedisCommand> writeQueue = FQueue.emptyQueue

    }

    private volatile State state = []

    RedisClient(SocketAddress remoteAddress, ChannelFactory factory = null) {
        super(remoteAddress, factory)
    }

    ChannelPipeline getPipeline() {
        def pipeline = super.getPipeline()

        pipeline.removeFirst() // remove self

        pipeline.addLast("redis.client", this)
        pipeline
    }

    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        def m = e.getMessage()
        if (!(m instanceof ChannelBuffer)) {
            ctx.sendUpstream(e)
            return
        }

        ChannelBuffer input = m
        if (!input.readable()) {
            return
        }

        ChannelBuffer cumulation = cumulation(ctx);
        if (cumulation.readable()) {
            cumulation.discardReadBytes();
            cumulation.writeBytes(input);
            callDecode(cumulation);
        } else {
            callDecode(input);
            if (input.readable()) {
                cumulation.writeBytes(input);
            }
        }
    }

    private void callDecode(ChannelBuffer cumulation) throws Exception {
        while (cumulation.readable()) {
            int oldReaderIndex = cumulation.readerIndex();
            def frame = state.readQueue.first.decode(cumulation, this);
            if (frame == MORE) {
                cumulation.readerIndex(oldReaderIndex)
                break
            } else if (oldReaderIndex == cumulation.readerIndex()) {
                throw new IllegalStateException(
                        "decode() method must read at least one byte " +
                        "if it returned a frame (caused by: " + getClass() + ")");
            }
        }
    }

    public BindLater get(String key, BindLater.Listener listener = null) {
        enqueue(new Get(key:key)).onBound(listener)
    }

    BindLater set(String key, def value, BindLater.Listener listener = null) {
        enqueue(new Set(key:key, value:value)).onBound(listener)
    }

    public <T> BindLater<T> getset(String key, def value, BindLater.Listener listener = null) {
        enqueue(new GetSet(key:key, value:value)).onBound(listener)
    }

    BindLater flushDb(BindLater.Listener listener = null) {
        enqueue(new FlushDb()).onBound(listener)
    }

    BindLater<Boolean> exists(String key, BindLater.Listener<Boolean> listener = null) {
        enqueue(new Exists(key:key)).whenBound(listener)
    }

    BindLater<Integer> del(List<String> keys, BindLater.Listener<Integer> listener = null) {
        enqueue(new Del(keys:keys)).onBound(listener)
    }

    BindLater<Boolean> rename(String oldKey, String newKey, BindLater.Listener<Boolean> listener = null) {
        enqueue(new Rename(oldKey:oldKey, newKey:newKey)).whenBound(listener)
    }

    BindLater<List> mget(List<String> keys, BindLater.Listener<List> listener = null) {
        enqueue(new Mget(keys:keys)).onBound(listener)
    }

    BindLater<List> mset(List keysValues, BindLater.Listener<List> listener = null) {
        enqueue(new Mset(keysValues:keysValues)).onBound(listener)
    }

    protected RedisCommand enqueue(RedisCommand command) {
        for(;;) {
            def s = state
            State ns = new State ()

            ns.writeQueue = s.writeQueue + command
            ns.readQueue  = s.readQueue  + command

            if(state.compareAndSet(s, ns)) {
                if (s.writeQueue.empty) {
                    command.write(channel).addListener { writeCompleted() }
                }
                return command
            }
        }
    }

    private void writeCompleted() {
        for(;;) {
            def s = state
            def ns = new State()

            def removed = s.writeQueue.removeFirst()
            ns.writeQueue = removed.second
            ns.readQueue  = s.readQueue

            if(state.compareAndSet(s, ns)) {
//                println "${removed.first} written"
                if(!ns.writeQueue.empty) {
                    ns.writeQueue.first.write(channel).addListener{ writeCompleted() }
                }
                return
            }
        }
    }

    public void readCompleted() {
        for (;;) {
            def s = state
            State ns = new State()

            ns.writeQueue = s.writeQueue
            def removed = s.readQueue.removeFirst()
            ns.readQueue  = removed.second

            if(state.compareAndSet(s, ns)) {
//                println "${removed.first} completed"
                return
            }
        }
    }

    private ChannelBuffer cumulation;

    private ChannelBuffer cumulation(ChannelHandlerContext ctx) {
        ChannelBuffer c = cumulation;
        if (c == null) {
            c = ChannelBuffers.dynamicBuffer(ctx.getChannel().getConfig().getBufferFactory());
            cumulation = c;
        }
        return c;
    }
}
