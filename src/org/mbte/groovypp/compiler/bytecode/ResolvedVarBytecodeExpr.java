package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.classgen.Variable;
import org.mbte.groovypp.compiler.CompilerTransformer;

public class ResolvedVarBytecodeExpr extends ResolvedLeftExpr {
    private final VariableExpression ve;
    private final Variable var;

    public ResolvedVarBytecodeExpr(ClassNode type, VariableExpression ve, CompilerTransformer compiler) {
        super(ve, type);
        this.ve = ve;
        var = compiler.compileStack.getVariable(ve.getName(), true);
    }

    protected void compile() {
        load(var);
    }

    public BytecodeExpr createAssign(ASTNode parent, final BytecodeExpr right, CompilerTransformer compiler) {
        final ClassNode vtype;
        if (ve.getAccessedVariable().isDynamicTyped()) {
            vtype = ClassHelper.getWrapper(right.getType());
            compiler.getLocalVarInferenceTypes().add(ve, vtype);
        }
        else {
            vtype = right.getType();
        }

        return new BytecodeExpr(parent, vtype) {
            protected void compile() {
                right.visit(mv);
                box(right.getType());
                cast(ClassHelper.getWrapper(right.getType()), ClassHelper.getWrapper(getType()));
                unbox(getType());
                storeVar(var);
                load(var);
            }
        };
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, BytecodeExpr right, int type, CompilerTransformer compiler) {
        return new BytecodeExpr(parent, getType()) {
            protected void compile() {
            }
        };
    }

    public BytecodeExpr createPrefixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }

    public BytecodeExpr createPostfixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }
}
