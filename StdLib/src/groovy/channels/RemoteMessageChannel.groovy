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

package groovy.channels

import java.util.concurrent.ConcurrentHashMap

@Typed class RemoteMessageChannel<M extends Serializable> extends MessageChannel<M> {
    private UUID hostId
    private UUID serialId

    private transient IOContext ioContext

    protected Object writeReplace () {
        this
    }

    void post(M message) {
        ioContext.post (new ForwardMessageToBeSent(forwardTo:serialId, message:message))
    }

    void writeExternal(ObjectOutput out) {
        out.writeObject(hostId)
        out.writeObject(serialId)
    }

    /**
     * Serialized form of MessageChannel
     */
    static class SerializedMessageChannel implements Externalizable {
        private UUID hostId
        private UUID serialId

        SerializedMessageChannel() {
        }

        SerializedMessageChannel(MessageChannel channel) {
            def context = IOContext.context.get()
            assert context

            serialId = context.makeRemoteable(channel).serialID
            hostId   = context.myHost
        }

        public Object readResolve () {
            def context = IOContext.context.get()
            assert context

            if (hostId == context.myHost) {
                context.getBySerialId(serialId)
            }
            else {
                context.getRemoteBySerialId(serialId)
            }
        }

        void writeExternal(ObjectOutput out) {
            out.writeLong hostId.mostSignificantBits
            out.writeLong hostId.leastSignificantBits
            out.writeLong serialId.mostSignificantBits
            out.writeLong serialId.leastSignificantBits
        }

        void readExternal(ObjectInput inp) {
            hostId = new UUID(inp.readLong(), inp.readLong())
            serialId = new UUID(inp.readLong(), inp.readLong())
        }
    }

    /**
     * IOContext represents one end of established connection between two hosts
     */
    abstract static class IOContext extends ExecutingChannel {

        MessageChannel mainActor

        static final UUID MAIN_ACTOR_ID = [0L,0L]
        
        UUID foreignHost
        UUID myHost

        private CachingClassLoader classLoader = [IOContext.class.classLoader]

        private final ConcurrentHashMap<MessageChannel, Remoteable> exposed = []
        private final ConcurrentHashMap<UUID, Remoteable> reversed = []
        private final RemoteChannelMap remote = [this]

        private static final Remoteable dummy = [null, null]

        Remoteable makeRemoteable(MessageChannel object) {
            def res = exposed.get(object)
            if(!res) {
                def id = UUID.randomUUID()
                while (reversed.putIfAbsent(id, dummy)) {
                    id = UUID.randomUUID()
                }
                res = new Remoteable(object, id)
                def pif = exposed.putIfAbsent(object, res)
                if (pif) {
                    res = pif
                    reversed.remove(id)
                }
                else {
                    reversed.put(id, res)
                }
            }
            res.addRef()
        }

        static ThreadLocal<IOContext> context = []

        abstract void sendBytes(byte [] bytes)

        protected void onMessage(def message) {
            switch(message) {
                case ForwardMessageToBeSent:
                        sendOverWire(new ForwardMessageReceived(forwardTo:message.forwardTo, message:message.message))
                    break

                case ForwardMessageReceived:
                       getBySerialId(message.forwardTo)?.post(message.message) 
                    break

                default:
                    super.onMessage(message)
            }
        }

        protected void sendOverWire(def object) {
            assert !IOContext.context.get()
            def bos = new ByteArrayOutputStream()
            try {
                IOContext.context.set(this)
                def out = new SerialOutputStream(bos)
                out.writeObject object
                // we don't close with purpose
                // out.close()
            }
            finally {
                IOContext.context.remove()
            }

            sendBytes(bos.toByteArray())
        }

        void receiveBytes(byte [] bytes) {
            assert !IOContext.context.get()
            Pair<UUID,Object> msg
            try {
                IOContext.context.set(this)
                def inp = new SerialInputStream(new ByteArrayInputStream(bytes), classLoader)
                def received = inp.readObject()
                post(received)
                // we don't close with purpose
                // inp.close ()
            }
            finally {
                IOContext.context.remove()
            }

            if (msg)
                getBySerialId(msg.first)?.post(msg.second)
        }

        MessageChannel getBySerialId (UUID serialId) {
            MAIN_ACTOR_ID == serialId ? mainActor : reversed.get(serialId)?.object
        }

        RemoteMessageChannel getRemoteBySerialId(UUID serialId) {
            def res = remote.get(serialId)?.get()
            res ? res : remote.putIfNotYet(serialId, new RemoteMessageChannel(hostId:foreignHost, serialId:serialId, ioContext:this))
        }

        void sendForget(UUID uuid) {
            return 
        }
    }

    private static class RemoteChannelMap extends WeakValueMap<UUID,RemoteMessageChannel> {
        private IOContext ioContext

        RemoteChannelMap(IOContext ioContext) {
            this.ioContext = ioContext
        }

        boolean finalizeValueRef (WeakValueMap.WeakValue<UUID,RemoteMessageChannel> ref) {
            if(super.finalizeValueRef(ref)) {
//                ioContext.sendForget(key)
            }
        }
    }

    static class Remoteable<T> {
        private final T object

        private transient volatile int refs

        UUID serialID

        Remoteable(T object, UUID id) {
            this.object = object
            this.serialID = id
        }

        Remoteable<T> addRef () {
            refs.incrementAndGet()
            this
        }

        void releaseRef () {
            if(refs.decrementAndGet() == 0) {
                // @todo
            }
        }
    }

    static class ForwardMessageToBeSent implements Externalizable {
        UUID   forwardTo
        Object message
    }

    /**
     * Message to be forwarded
     */
     static class ForwardMessageReceived implements Externalizable {
         UUID   forwardTo
         Object message
     }
}
