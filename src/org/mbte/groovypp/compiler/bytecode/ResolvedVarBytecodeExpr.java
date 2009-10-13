package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.classgen.Variable;
import org.codehaus.groovy.syntax.Token;
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
        if (ve.isDynamicTyped()) {
            vtype = ClassHelper.getWrapper(right.getType());
            compiler.getLocalVarInferenceTypes().add(ve, vtype);
        } else {
            vtype = ve.getType();
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

    public BytecodeExpr createBinopAssign(ASTNode parent, Token method, final BytecodeExpr right, CompilerTransformer compiler) {
        final BinaryExpression op = new BinaryExpression(this, method, right);
        op.setSourcePosition(parent);
        return createAssign(parent, (BytecodeExpr) compiler.transform(op), compiler);
    }

    public BytecodeExpr createPrefixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }

    public BytecodeExpr createPostfixOp(ASTNode parent, int type, CompilerTransformer compiler) {
        return null;
    }
}
