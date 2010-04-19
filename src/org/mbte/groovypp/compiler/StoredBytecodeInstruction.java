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

package org.mbte.groovypp.compiler;

import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.objectweb.asm.MethodVisitor;
import org.mbte.groovypp.compiler.asm.*;

public class StoredBytecodeInstruction extends BytecodeInstruction {

    private final StoringMethodVisitor storage = new StoringMethodVisitor ();

    public void visit(MethodVisitor mv) {
        for(AsmInstr op : storage.operations)
            op.visit(mv);
        storage.operations.clear();
    }

    public MethodVisitor createStorage() {
        return storage;
    }

    void clear () {
        storage.operations.clear();
    }
}