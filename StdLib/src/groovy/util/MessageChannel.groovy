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

package groovy.util

import groovy.util.concurrent.BindLater
import java.util.concurrent.Executor
import groovy.util.concurrent.FairExecutingChannel
import groovy.util.concurrent.NonfairExecutingChannel

@Typed abstract class MessageChannel<T> {

    abstract void post (T message)

    static <M extends ReplyRequiringMessage, R> void request(MessageChannel<M> channel, M message, MessageChannel<R> replyTo) {
        message.replyTo = replyTo
        channel.post(message)
    }

    static <M extends ReplyRequiringMessage, R> Object requestAndWait(MessageChannel<M> channel, M message) {
        def binder = new BindLater()
        channel.request(message) { reply ->
            binder.set(reply)
        }
        binder.get()
    }

    final MessageChannel<T> leftShift (T message) {
        post message
        this
    }

    final MessageChannel<T> addBefore(MessageChannel<T> other) {
        def that = this;
        { message ->
            other.post message
             that.post message
        }
    }

    final MessageChannel<T> addAfter(MessageChannel<T> other) {
        def that = this;
        { message ->
             that.post message
            other.post message
        }
    }

    final MessageChannel<T> filter(Function1<T,Boolean> filter) {
        def that = this;
        { message ->
            if (filter(message))
                that.post(message)
        }
    }

    final <R> MessageChannel<R> map(Function1<T,R> mapping) {
        def that = this;
        { message ->
            that.post(mapping(message))
        }
    }

    final MessageChannel<T> async(Executor executor, boolean fair = false) {
        def that = this;
        if (fair) {
            (FairExecutingChannel)[
                executor:executor,
                onMessage: { message ->
                   that.post(message)
                }
            ]
        }
        else {
            (NonfairExecutingChannel)[
                executor:executor,
                onMessage: { message ->
                   that.post(message)
                }
            ]
        }
    }

    static class ReplyRequiringMessage {
        MessageChannel replyTo
    }
}