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
            if (other.elementCount == -1) {
                labelStack = new byte [0];
            }
            else {
                labelStack = new byte [other.elementCount];
                for (int i = 0; i != other.elementCount; ++i)
                    labelStack[i] = other.elements[i];
            }
        }
        else {
            if (other.elementCount == -1) {
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
