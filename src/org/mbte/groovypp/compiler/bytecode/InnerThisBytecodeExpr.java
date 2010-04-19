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

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.objectweb.asm.MethodVisitor;

public class InnerThisBytecodeExpr extends BytecodeExpr {
    private final ClassNode innerClass;
    private final ClassNode outerClass;
    private final CompilerTransformer compiler;

    public InnerThisBytecodeExpr(ASTNode parent, ClassNode outerClass, CompilerTransformer compiler) {
        this(parent, outerClass, compiler, compiler.classNode);
    }

    public InnerThisBytecodeExpr(ASTNode parent, ClassNode outerClass, CompilerTransformer compiler, ClassNode innerClass) {
        super(parent, outerClass);
        this.outerClass = outerClass.redirect();
        this.compiler = compiler;
        this.innerClass = innerClass;
    }

    public boolean isThis() {
        return innerClass.equals(outerClass);
    }

    protected void compile(MethodVisitor mv) {
        mv.visitVarInsn(ALOAD, 0);
        ClassNode curThis = innerClass;
        while (!curThis.equals(outerClass)) {
            compiler.context.setOuterClassInstanceUsed(curThis);
            ClassNode next = curThis.getField("this$0").getType();
            mv.visitFieldInsn(GETFIELD, BytecodeHelper.getClassInternalName(curThis), "this$0", BytecodeHelper.getTypeDescription(next));
            curThis = next;
        }
    }
}
