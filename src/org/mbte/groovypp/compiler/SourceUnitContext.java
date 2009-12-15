package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ven
 */
public class SourceUnitContext {
    private int syntheticAccessorNumber = 1979;
    public Map<FieldNode, MethodNode> generatedFieldGetters = new HashMap<FieldNode, MethodNode>();
    public Map<FieldNode, MethodNode> generatedFieldSetters = new HashMap<FieldNode, MethodNode>();
    public Map<MethodNode, MethodNode> generatedMethodDelegates = new HashMap<MethodNode, MethodNode>();

    public MethodNode getFieldGetter(FieldNode field) {
        MethodNode getter = generatedFieldGetters.get(field);
        if (getter == null) {
            int num = syntheticAccessorNumber++;
            String getterName = "getField" + num;
            int modifiers = field.getModifiers() & Opcodes.ACC_STATIC;
            getter = new MethodNode(getterName, modifiers, field.getType(), new Parameter[0], new ClassNode[0],
                    new ReturnStatement(new VariableExpression(field)));
            ClassNode clazz = field.getDeclaringClass();
            getter.setDeclaringClass(clazz);
            clazz.addMethod(getter);
            generatedFieldGetters.put(field, getter);
            String setterName = "setField" + num;
            Parameter[] setterParams = {new Parameter(field.getType(), "p")};
            ExpressionStatement code = new ExpressionStatement(new BinaryExpression(new VariableExpression(field),
                                                               Token.newSymbol(Types.ASSIGN, -1, -1),
                                                               new VariableExpression("p")));
            MethodNode setter = new MethodNode(setterName, modifiers, ClassHelper.VOID_TYPE, setterParams, new ClassNode[0],
                    code);
            setter.setDeclaringClass(clazz);
            clazz.addMethod(setter);
            generatedFieldSetters.put(field, setter);
            ClassNodeCache.clearCache(clazz);
        }
        return getter;
    }

    public MethodNode getMethodDelegate(MethodNode method) {
        MethodNode delegate = generatedMethodDelegates.get(method);
        if (delegate == null) {
            int num = syntheticAccessorNumber++;
            String getterName = "delegate" + num;
            int modifiers = method.getModifiers() & Opcodes.ACC_STATIC;

            Expression[] exprs = new Expression[method.getParameters().length];
            for (int i = 0; i < exprs.length; i++) {
                exprs[i] = new VariableExpression(method.getParameters()[i]);
            }
            Expression argList = new ArgumentListExpression(exprs);
            delegate = new MethodNode(getterName, modifiers, method.getReturnType(), method.getParameters(), new ClassNode[0],
                    new ReturnStatement(new MethodCallExpression(new VariableExpression("this", ClassHelper.DYNAMIC_TYPE),
                            method.getName(), argList)));
            generatedMethodDelegates.put(method, delegate);
            ClassNode clazz = method.getDeclaringClass();
            delegate.setDeclaringClass(clazz);
            clazz.addMethod(delegate);
            ClassNodeCache.clearCache(clazz);
        }
        return delegate;
    }
}
