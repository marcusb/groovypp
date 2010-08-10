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

@Typed class Del extends RedisCommand<Integer> {
    List<String> keys

    protected ChannelFuture write(Channel channel) {
        StringBuilder sb = ["DEL "]
        for(k in keys) {
            sb << " "
            sb << k
        }
        sb << "\r\n"
        channel.write(sb.toString().channelBuffer)
    }

    protected void buildCommand (RedisCommandBuilder command) {
        for(k in keys)
            command.addString(k)
    }

    def decode(ChannelBuffer msg, RedisClient redis) {
        decodeInteger(msg, redis)
    }

    String toString () { "DEL($keys)" }
}
