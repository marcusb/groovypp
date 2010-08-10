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

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.buffer.CompositeChannelBuffer
import java.nio.ByteOrder

@Typed class Mset extends RedisCommand<Integer> {
    List keysValues

//    protected ChannelFuture write(Channel channel) {
//        List buffers = ["*${keysValues.size()+1}\r\n\$4\r\nMSET".channelBuffer]
//        for(int i = 0; i < keysValues.size(); i += 2) {
//            def key = keysValues[i].toString()
//            def value = keysValues[i+1].toSerialBytes()
//            buffers << "\r\n\$${key.length()}\r\n$key\r\n\$${value.length}\r\n".channelBuffer
//            buffers << ChannelBuffers.wrappedBuffer(value)
//        }
//        buffers << CRLF
//        channel.write(new CompositeChannelBuffer(ByteOrder.BIG_ENDIAN,buffers))
//    }
//
    protected void buildCommand (RedisCommandBuilder command) {
        for(int i = 0; i < keysValues.size(); i += 2) {
            def key = keysValues[i].toString()
            def value = keysValues[i+1]
            command.addString(key.toString())
            command.addBytes(value)
        }
    }

    def decode(ChannelBuffer msg, RedisClient redis) {
        decodePlusOrMinus(msg, redis)
    }

    String toString () { "MSET" }
}
