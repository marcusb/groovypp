package org.mbte.gretty.redis

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.buffer.CompositeChannelBuffer
import java.nio.ByteOrder
import org.jboss.netty.buffer.AbstractChannelBuffer

@Typed class RedisCommandBuilder {
    private ChannelBuffer collected = ChannelBuffers.dynamicBuffer()
    private int count
    
    RedisCommandBuilder() {
        collected.writerIndex(32) // reserve for *n\r\n
    }

    void addString(String command) {
        count++
        collected.writeBytes("\$${command.length()}\r\n$command\r\n".bytes)
    }

    void addBytes(Object obj) {
        count++
        def bytes = obj.toSerialBytes()
        collected.writeBytes("\$${bytes.length}\r\n".bytes)
        collected.writeBytes(bytes)
        collected.writeBytes(RedisCommand.CRLF.array())
    }

    ChannelBuffer commit () {
        def toAdd = "*$count\r\n".bytes
        def pos = 32 - toAdd.length
        System.arraycopy(toAdd, 0, collected.array(), pos, toAdd.length)
        collected.readerIndex(pos)
        collected
    }
}
