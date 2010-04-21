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

class CompareEqualsTest extends GroovyShellTestCase {
  void testEqualsOperatorIsMultimethodAware() {
    shell.evaluate("""

        @Typed
        class Xyz {
            boolean equals(Xyz other) {
                true
            }

            boolean equals(Object other) {
                null
            }

            boolean equals(String str) {
                str.equalsIgnoreCase this.class.getName()
            }
        }

        @Typed
        def u() {
          assert new Xyz() == new Xyz()
          assert new Xyz().equals(new Xyz())
          assert !(new Xyz() == 239)
        }
        u();
      """
    )

  }
}