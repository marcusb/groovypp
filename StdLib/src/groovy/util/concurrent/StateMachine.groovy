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

//package groovy.util.concurrent
//
//import groovy.channels.ExecutingChannel
//import groovy.channels.MessageChannel
//
//class StateMachine extends ExecutingChannel {
//  private final HashMap<String,AbstractState> stateMap
//  private def curMessage
//  private AbstractState curState
//
//  StateMachine() {
//  }
//
//  void setStates (List<AbstractState> states) {
//    assert !this.states
//
//    stateMap = [:]
//    for(c in states) {
//      stateMap [c.name] = c
//      c.stateMachine = this
//    }
//    curState = states[0]
//  }
//
//  protected void onMessage(msg) {
//      assert curState
//
//      curMessage = msg
//      curState.onMessage (msg)
//      curMessage = null
//  }
//
//  static class AbstractState {
//      String name
//
//      StateMachine stateMachine
//
//      void onEnter () {}
//      void onLeave () {}
//
//      void onMessage (def msg) {
//        throw new IllegalStateException("State:'$name', message:$msg")
//      }
//
//      protected void transit(AbstractState newState) {
//          assert stateMachine.curState == this
//          assert !newState
//
//          onLeave()
//          stateMachine.curState = newState
//          newState.onEnter()
//      }
//  }
//}
//
//class JointWork extends StateMachine {
//  protected final List<ParticipantState> participants = []
//
//  static class ParticipantState {
//    MessageChannel participant
//    boolean confirmed
//    boolean ready
//  }
//
//  static class JointWorkMesage {
//    JointWork work
//  }
//
//  static class DefineParticipants extends JointWorkMesage {
//    List<MessageChannel> participants
//  }
//
//  static class Invite extends JointWork {}
//
//  static class InvitationConfirmation extends JointWork {
//    MessageChannel participant
//    boolean confirm
//  }
//
//  static class SuggestStart extends JointWork {}
//}
//
//JointWork stateMachine = [
//  states: [
//    [
//      name: 'initial',
//      onMessage: { msg ->
//        switch(msg) {
//          case JointWork.DefineParticipants:
//              for (p in msg.participants)
//                work.participants << [participant:p]
//              transit 'invitationSent'
//            break;
//
//          default:
//            super.onMessage(msg)
//        }
//      }
//    ],
//    [
//       name: 'invitationSent',
//       onEnter: {
//         JointWork work = stateMachine
//         for (p in work.participants) {
//           p.participant << new JointWork.Invite(work:work)
//         }
//       },
//       onMessage { msg ->
//         switch(msg) {
//           case JointWork.InvitationConfirmation:
//              if (!msg.confirmed) {
//                transit 'jobFailed'
//              }
//              else {
//                JointWork work = stateMachine
//                work.participants.find { it.participant == msg.participant}.confirmed = true
//
//                if (!work.participants.any{ !it.confirmed }) {
//                  transit 'confirmationReceived'
//                }
//              }
//             break;
//
//           default:
//             super.onMessage(msg)
//         }
//       },
//    ],
//    [
//       name: 'confirmationReceived',
//       onEnter: {
//         JointWork work = stateMachine
//         for (p in work.participants) {
//           p.participant << new JointWork.SuggestStart(work:work)
//         }
//       },
//       onMessage { msg ->
//        switch(msg) {
//          case JointWork.InvitationConfirmation:
//             if (!msg.confirmed) {
//               transit 'jobFailed'
//             }
//             else {
//               JointWork work = stateMachine
//               work.participants.find { it.participant == msg.participant}.confirmed = true
//
//               if (!work.participants.any{ !it.confirmed }) {
//                 transit 'confirmationReceived'
//               }
//             }
//            break;
//
//          default:
//            super.onMessage(msg)
//        }
//       },
//    ],
//    [
//       name: 'everybodyReady',
//    ],
//    [
//       name: 'jobDone',
//       onMessage { msg -> }
//    ],
//    [
//       name: 'jobFailed',
//       onMessage { msg -> }
//    ],
//  ]
//]
