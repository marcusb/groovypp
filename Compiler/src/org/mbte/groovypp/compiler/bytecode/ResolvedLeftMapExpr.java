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
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.syntax.Token;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.objectweb.asm.MethodVisitor;

public class ResolvedLeftMapExpr extends ResolvedLeftExpr {
    private final BytecodeExpr object;
    private final String propName;

    public ResolvedLeftMapExpr(ASTNode parent, BytecodeExpr object, String propName) {
        super(parent, getMapValueType(object));
        this.object = object;
        this.propName = propName;
    }

    private static ClassNode getMapValueType(BytecodeExpr object) {
        final ClassNode substitutedType = TypeUtil.getSubstitutedType(ClassHelper.MAP_TYPE, ClassHelper.MAP_TYPE, object.getType());
        return substitutedType.getGenericsTypes() != null ? substitutedType.getGenericsTypes()[1].getType() : ClassHelper.OBJECT_TYPE;
    }

    public BytecodeExpr createAssign(ASTNode parent, final BytecodeExpr right, CompilerTransformer compiler) {
        final CastExpression cast = new CastExpression(getType(), right);
        cast.setSourcePosition(right);
        final BytecodeExpr newRight = (BytecodeExpr) compiler.transformToGround(cast);
        return new BytecodeExpr(parent, getType()) {
            protected void compile(MethodVisitor mv) {
                object.visit(mv);
                mv.visitLdcInsn(propName);
                newRight.visit(mv);
                dup_x2(getType(), mv);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
                mv.visitInsn(POP);
            }
        };
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token method, BytecodeExpr right, CompilerTransformer compiler) {
        return null;
    }

    protected void compile(MethodVisitor mv) {
        object.visit(mv);
        mv.visitLdcInsn(propName);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
        checkCast(getType(), mv);
    }
}
