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

/**
 * Channel, which decide(select) for each incoming message, who can be interested in this message
 * and forward it for processing.
 *
 * For example, some selector may choose lazily create and cache actual recipients of the message.
 * Such strategy is implemented in CachingSelectorChannel
 */
abstract class SelectorChannel<M> extends MessageChannel<M> {

  final void post(M message) {
    for (c in selectInterested(message))
      c.post(message)
  }

  abstract Iterator<MessageChannel<M>> selectInterested(M message);
}
