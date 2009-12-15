package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
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
            String name = "access" + (syntheticAccessorNumber++);
            int modifiers = field.getModifiers() & Opcodes.ACC_STATIC;
            getter = new MethodNode(name, modifiers, field.getType(), new Parameter[0], new ClassNode[0],
                    new ReturnStatement(new VariableExpression(field)));
            ClassNode clazz = field.getDeclaringClass();
            getter.setDeclaringClass(clazz);
            clazz.addMethod(getter);
            generatedFieldGetters.put(field, getter);
        }
        return getter;
    }
}
