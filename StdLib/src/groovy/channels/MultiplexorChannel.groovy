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

import groovy.util.concurrent.FList

@Typed class MultiplexorChannel<M> extends SelectorChannel<M> {
    private volatile FList<MessageChannel<M>> listeners = FList.emptyList

    MultiplexorChannel() {
    }

    MultiplexorChannel(MessageChannel<M> channel) {
        subscribe(channel)
    }

    MultiplexorChannel<M> subscribe(MessageChannel<M> channel) {
        for (;;) {
            def l = listeners
            if (listeners.compareAndSet(l, l + channel))
                return this
        }
    }

    MultiplexorChannel<M> subscribe(MessageChannel<M> ... channels) {
        for (c  in channels) {
            subscribe(c)
        }
        this
    }

    MultiplexorChannel<M> unsubscribe(MessageChannel<M> channel) {
        for (;;) {
            def l = listeners
            if (listeners.compareAndSet(l, l - channel))
                return this
        }
    }

    static MultiplexorChannel<M> of (MessageChannel<M> ... channels) {
        new MultiplexorChannel().subscribe(channels)
    }

    Iterator<MessageChannel<M>> selectInterested(M message) {
        listeners.iterator()
    }
}
