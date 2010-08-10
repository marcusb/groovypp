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

@Typed class Set extends RedisCommand {
    private static final ChannelBuffer SET = ChannelBuffers.wrappedBuffer("SET".bytes)

    String   key
    def      value

    protected ChannelFuture write(Channel channel) {
        def bytes = value.toSerialBytes()
        channel.write(SET, " $key ${bytes.length}\r\n".channelBuffer, ChannelBuffers.wrappedBuffer(bytes), CRLF)
    }

    protected void buildCommand(RedisCommandBuilder command) {
        command.addString(key)
        command.addBytes(value)
    }

    def decode(ChannelBuffer msg, RedisClient redis) {
        decodePlusOrMinus(msg, redis)
    }

    String toString () { "SET($key)"}
}

