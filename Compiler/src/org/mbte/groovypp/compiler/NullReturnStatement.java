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

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class NullReturnStatement extends BytecodeSequence implements Opcodes {
    @Override
    public void visit(GroovyCodeVisitor visitor) {
        if (visitor instanceof StaticCompiler)
          ((StaticCompiler)visitor).visitBytecodeSequence (this);
        else
          super.visit(visitor);
    }

    public NullReturnStatement(final ClassNode returnType) {
        super(new BytecodeInstruction(){
            public void visit(MethodVisitor mv) {
                if (returnType == ClassHelper.double_TYPE) {
                    mv.visitLdcInsn((double)0);
                    mv.visitInsn(DRETURN);
                } else if (returnType == ClassHelper.float_TYPE) {
                    mv.visitLdcInsn((float)0);
                    mv.visitInsn(FRETURN);
                } else if (returnType == ClassHelper.long_TYPE) {
                    mv.visitLdcInsn(0L);
                    mv.visitInsn(LRETURN);
                } else if (
                       returnType == ClassHelper.boolean_TYPE
                    || returnType == ClassHelper.char_TYPE
                    || returnType == ClassHelper.byte_TYPE
                    || returnType == ClassHelper.int_TYPE
                    || returnType == ClassHelper.short_TYPE) {
                    //byte,short,boolean,int are all IRETURN
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(IRETURN);
                } else if (returnType == ClassHelper.VOID_TYPE) {
                    mv.visitInsn(RETURN);
                } else {
                    mv.visitInsn(ACONST_NULL);
                    mv.visitInsn(ARETURN);
                }
            }
        });
    }
}
