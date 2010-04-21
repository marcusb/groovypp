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

package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

public class ClassExpressionTransformer extends ExprTransformer<ClassExpression> {
    public Expression transform(ClassExpression exp, CompilerTransformer compiler) {

        final ClassNode type = exp.getType();
        return new ClassExpr(exp, type);
    }

    public static BytecodeExpr newExpr(Expression exp, ClassNode type) {
        return new ClassExpr(exp, type);
    }

    private static class ClassExpr extends BytecodeExpr {
        private final ClassNode type;

        public ClassExpr(Expression exp, ClassNode type) {
            super(exp, TypeUtil.withGenericTypes(ClassHelper.CLASS_Type, type));
            this.type = type;
        }

        protected void compile(MethodVisitor mv) {
            if (ClassHelper.isPrimitiveType(type)) {
                mv.visitFieldInsn(GETSTATIC, BytecodeHelper.getClassInternalName(TypeUtil.wrapSafely(type)), "TYPE", "Ljava/lang/Class;");
            } else {
                mv.visitLdcInsn(BytecodeHelper.getClassLoadingTypeDescription(type));
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
            }
        }
    }
}
