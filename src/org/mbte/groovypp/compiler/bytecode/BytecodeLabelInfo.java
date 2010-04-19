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

package org.mbte.groovypp.compiler.bytecode;

public class BytecodeLabelInfo {
    private byte labelStack [];

    boolean isSameStack (BytecodeStack other) {
        if (labelStack.length != other.elementCount)
            return false;

        for (int i = 0; i != labelStack.length; ++i)
            if (labelStack[i] != other.elements[i])
                return false;

        return true;
    }

    void initFromStack(BytecodeStack other) {
        if (labelStack == null) {
            if (other.elementCount == 0) {
                labelStack = new byte [0];
            }
            else {
                labelStack = new byte [other.elementCount];
                for (int i = 0; i != other.elementCount; ++i)
                    labelStack[i] = other.elements[i];
            }
        }
        else {
            if (other.elementCount == 0) {
                // it happens after GOTO/RETURN etc.
                other.elements = new byte [Math.max(other.elements.length, labelStack.length)];
                for (int i = 0; i != labelStack.length; ++i)
                    other.elements[i] = labelStack[i];
                other.elementCount = labelStack.length;
            }
            else
                if (!isSameStack(other))
                    throw new RuntimeException("Inconsistent stack");
        }
    }
}
