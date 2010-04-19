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

import groovy.lang.TypePolicy;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

public class CompiledClosureBytecodeExpr extends BytecodeExpr {
    private CompilerTransformer compiler;

    public CompiledClosureBytecodeExpr(CompilerTransformer compiler, ClosureExpression ce, ClassNode newType) {
        super(ce, newType);
        this.compiler = compiler;

        ClosureUtil.addFields(ce, newType, compiler);

        compiler.pendingClosures.add(this);
    }

    protected void compile(MethodVisitor mv) {
        ClassNode type = getType();
        if (compiler.policy == TypePolicy.STATIC && !compiler.context.isOuterClassInstanceUsed(type) &&
                type.getDeclaredField("this$0") != null /* todo: remove this check */) {
            type.removeField("this$0");
        }
        Parameter[] constrParams = ClosureUtil.createClosureConstructorParams(type, compiler);
        ClosureUtil.createClosureConstructor(type, constrParams, null, compiler);
        ClosureUtil.instantiateClass(type, compiler, constrParams, null, mv);
    }

    public static Expression createCompiledClosureBytecodeExpr(final CompilerTransformer transformer, final ClosureExpression ce) {
        final ClassNode newType = ce.getType();
        return new CompiledClosureBytecodeExpr(transformer, ce, newType);
    }
}
