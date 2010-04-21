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

class BytecodeStack {
    public static final byte KIND_INT    = 0;
    public static final byte KIND_OBJ    = 1;
    public static final byte KIND_LONG   = 2;
    public static final byte KIND_DOUBLE = 3;
    public static final byte KIND_FLOAT  = 4;

    protected byte[] elements  = new byte [8];
    protected int elementCount;

    public void clear() {
        elementCount = 0;
    }

    void push (byte kind) {
        elements[elementCount++] = kind;
        if (elementCount == elements.length) {
            byte [] newElements = new byte [elements.length * 2];
            System.arraycopy(elements, 0, newElements, 0, elementCount);
            elements = newElements;
        }
    }

    void pop (byte kind) {
        int e = elements[--elementCount];
        if (e != kind) {
            throw new RuntimeException("Inconsistent pop");
        }
    }

    void pop2 () {
        int e = elements[--elementCount];
        if (e != KIND_DOUBLE && e != KIND_LONG) {
            throw new RuntimeException("Inconsistent pop");
        }
    }

    public byte pop() {
        byte e = elements[--elementCount];
        if (e == KIND_LONG || e == KIND_DOUBLE) {
            throw new RuntimeException("Inconsistent pop");
        }
        return e;
    }

    public void dup() {
        byte e = pop ();
        push(e);
        push(e);
    }

    public void dup_x1() {
        byte se1 = pop (), se2 = pop ();
        push(se1);
        push(se2);
        push(se1);
    }

    public void dup_x2() {
        byte top = pop ();
        byte last = elements[--elementCount];
        if (last == KIND_LONG || last == KIND_DOUBLE) {
            push(top);
            push(last);
            push(top);
        }
        else {
            byte preLast = pop ();
            push(top);
            push(preLast);
            push(last);
            push(top);
        }
    }

    public void dup2() {
        byte last = elements[--elementCount];
        if (last == KIND_LONG || last == KIND_DOUBLE) {
            push(last);
            push(last);
        }
        else {
            byte preLast = pop ();
            push(preLast);
            push(last);
            push(preLast);
            push(last);
        }
    }

    public void dup2_x1() {
        byte last = elements[--elementCount];
        if (last == KIND_LONG || last == KIND_DOUBLE) {
            byte mid = pop ();
            push(last);
            push(mid);
            push(last);
        }
        else {
            byte preTop = pop (), mid = pop ();
            push(preTop);
            push(last);
            push(mid);
            push(preTop);
            push(last);
        }
    }

    public void dup2_x2() {
        byte last = elements[--elementCount];
        if (last == KIND_LONG || last == KIND_DOUBLE) {
            byte mid = elements[--elementCount];
            if (mid == KIND_DOUBLE || mid == KIND_LONG) {
                push(last);
                push(mid);
                push(last);
            }
            else {
                byte mid2 = pop ();
                push(last);
                push(mid2);
                push(mid);
                push(last);
            }
        }
        else {
            byte preTop = pop ();
            byte mid = elements[--elementCount];
            if (mid == KIND_DOUBLE || mid == KIND_LONG) {
                push(preTop);
                push(last);
                push(mid);
                push(preTop);
                push(last);
            }
            else {
                byte mid2 = pop ();
                push(preTop);
                push(last);
                push(mid2);
                push(mid);
                push(preTop);
                push(last);
            }
        }
    }

    public void swap() {
        byte top = pop ();
        byte preTop = pop ();
        push(top);
        push(preTop);
    }
}
