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

package groovy

/**
 * Checks that it's possible to call inner classes constructor from groovy
 * @author Guillaume Laforge
 */
class CallInnerClassCtorTest extends GroovyShellTestCase {

    void testCallCtor() {
      shell.evaluate("""
          @Typed
          def u() {
            def user = new groovy.OuterUser()
            user.name = "Guillaume"
            user.age = 27

            assert user.name == "Guillaume"
            assert user.age == 27
          }
          u();
        """
      )
    }

    void testCallInnerCtor() {
      shell.evaluate("""
          @Typed
          def u() {
            def address = new groovy.OuterUser.InnerAddress()
            address.city = "Meudon"
            address.zipcode = 92360

            assert address.city == "Meudon"
            assert address.zipcode == 92360
          }
          u();
        """
      )
    }

    void testCallInnerInnerCtor() {
      shell.evaluate("""
          @Typed
          def u() {
            def address = new groovy.OuterUser.InnerAddress.Street()
            address.name = "rue de la paix"
            address.number = 17

            assert address.name == "rue de la paix"
            assert address.number == 17
          }
          u();
        """
      )
    }
}