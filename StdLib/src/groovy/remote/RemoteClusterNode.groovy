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

package groovy.remote

/**
 * Remote node in the cluster
 */
@Typed class RemoteClusterNode extends MessageChannel<Serializable> {
   final RemoteConnection connection

   final UUID remoteId

   RemoteClusterNode (RemoteConnection connection, UUID remoteId) {
       this.connection = connection
       this.remoteId = remoteId
   }

   void post(Serializable message) {
        connection.send(new ToMainActor(payLoad:message))
   }

   @Typed public static class ToMainActor extends RemoteMessage {
        Serializable payLoad
   }
}
