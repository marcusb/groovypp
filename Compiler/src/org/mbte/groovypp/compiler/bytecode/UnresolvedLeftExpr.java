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
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.syntax.Token;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.objectweb.asm.MethodVisitor;

public class UnresolvedLeftExpr extends ResolvedLeftExpr {
    private final BytecodeExpr value;
    private final BytecodeExpr object;
    private final String propName;

    public UnresolvedLeftExpr(ASTNode exp, BytecodeExpr value, BytecodeExpr object, String propName) {
        super(exp, ClassHelper.DYNAMIC_TYPE);
        this.value = value;
        this.object = object;
        this.propName = propName;
    }

    protected void compile(MethodVisitor mv) {
        object.visit(mv);
        if (value == null) {
            // getter
            mv.visitLdcInsn(propName);
            mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper", "getProperty", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
        } else {
            // setter
            mv.visitLdcInsn(propName);
            value.visit(mv);
            mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper", "setProperty", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V");
        }
    }

    public BytecodeExpr createAssign(ASTNode parent, final BytecodeExpr right, CompilerTransformer compiler) {
        return new BytecodeExpr(parent, ClassHelper.getWrapper(right.getType())) {
            protected void compile(MethodVisitor mv) {
                object.visit(mv);
                mv.visitLdcInsn(propName);
                right.visit(mv);
                box(right.getType(), mv);
                mv.visitInsn(DUP_X2);
                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper", "setProperty", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V");
            }
        };
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token right, BytecodeExpr type, CompilerTransformer compiler) {
        return null;
    }

    public BytecodeExpr createPrefixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }

    public BytecodeExpr createPostfixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }
}
