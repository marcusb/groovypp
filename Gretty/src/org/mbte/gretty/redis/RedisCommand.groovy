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
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelFuture

@Typed abstract class RedisCommand<S> extends BindLater<S> {

    protected final static def MORE = []

    public static final ChannelBuffer CRLF = ChannelBuffers.wrappedBuffer("\r\n".bytes)

    protected ChannelFuture write(Channel channel) {
        RedisCommandBuilder command = []
        command.addString(getClass().simpleName)
        buildCommand(command)
        channel.write(command.commit())
    }

    protected abstract void buildCommand (RedisCommandBuilder command)

    abstract def decode(ChannelBuffer msg, RedisClient redis)

    final def decodePlusOrMinus(ChannelBuffer msg, RedisClient redis) {
        def first = readLine(msg)
        if (!first)
            return MORE

        redis.readCompleted()
        set(first.startsWith('+'))
    }

    final def decodeObject(ChannelBuffer msg, RedisClient redis) {
        def first = readLine(msg)
        if (!first)
            return MORE

        int len = Integer.parseInt(first.substring(1))
        if(len < 0) {
            redis.readCompleted()
            set(null)
        }
        else {
            if(msg.readableBytes() < len+2) {
                return MORE
            }
            def obj = Files.fromSerialBytes(msg.array(), msg.readerIndex(), len)
            msg.readerIndex (msg.readerIndex()+len+2)
            redis.readCompleted ()
            set(obj)
        }
    }

    final def decodeList(ChannelBuffer msg, RedisClient redis) {
        def first = readLine(msg)
        if (!first)
            return MORE

        List<Pair<Integer,Integer>> coll = []

        int len = Integer.parseInt(first.substring(1))
        for(int i = 0; i != len; ++i) {
            def line = readLine(msg)
            if(!line)
                return MORE

            int bytes = Integer.parseInt(line.substring(1))
            if(bytes < 0)
                coll << [-1,-1]
            else {
                if(msg.readableBytes() < bytes+2) {
                    return MORE
                }
                coll << [msg.readerIndex(), bytes]
                msg.readerIndex (msg.readerIndex()+bytes+2)
            }
        }

        redis.readCompleted()

        def res = []
        for(i in 0..<len) {
            def p = coll[i]
            if(p.first == -1)
                res << null
            else {
                res << Files.fromSerialBytes(msg.array(), p.first, p.second)
            }

        }
        set(res)
    }

    final def decodeBoolean(ChannelBuffer msg, RedisClient redis) {
        def first = readLine(msg)
        if (!first)
            return MORE

        def res = Integer.parseInt(first.substring(1))

        redis.readCompleted()
        set(res != 0)
    }

    final def decodeInteger(ChannelBuffer msg, RedisClient redis) {
        def first = readLine(msg)
        if (!first)
            return MORE

        def res = Integer.parseInt(first.substring(1))

        redis.readCompleted()
        set(res)
    }

    protected String readLine(ChannelBuffer msg) {
        def of = indexOf(msg, CRLF)
        if(of != -1) {
            def start = msg.readerIndex()
            msg.readerIndex (start+of+2)
            return new String(msg.array(), start, of, "UTF-8")
        }
        else {
            return null
        }
    }

    private static int indexOf(ChannelBuffer haystack, ChannelBuffer needle) {
        for (int i = haystack.readerIndex(); i < haystack.writerIndex(); i ++) {
            int haystackIndex = i;
            int needleIndex;
            for (needleIndex = 0; needleIndex < needle.capacity(); needleIndex ++) {
                if (haystack.getByte(haystackIndex) != needle.getByte(needleIndex)) {
                    break;
                } else {
                    haystackIndex ++;
                    if (haystackIndex == haystack.writerIndex() &&
                        needleIndex != needle.capacity() - 1) {
                        return -1;
                    }
                }
            }

            if (needleIndex == needle.capacity()) {
                // Found the needle from the haystack!
                return i - haystack.readerIndex();
            }
        }
        return -1;
    }

    static ChannelFuture write(Channel channel, ChannelBuffer[] buffers) {
        channel.write(ChannelBuffers.wrappedBuffer(buffers))
    }

    static ChannelBuffer getChannelBuffer(String self) {
        ChannelBuffers.wrappedBuffer(self.bytes)
    }

    BindLater<S> onBound(BindLater.Listener<S> listener) {
        if(!listener)
            return this

        whenBound {
            listener.onBound(this)
        }
    }
}
