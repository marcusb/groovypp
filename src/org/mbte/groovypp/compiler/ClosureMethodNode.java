package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

public class ClosureMethodNode extends MethodNode {
    private ClosureMethodNode owner;

    private List<Dependent> dependentMethods;

    public ClosureMethodNode(String name, int modifiers, ClassNode returnType, Parameter[] parameters, Statement code) {
        super(name, modifiers, returnType, parameters, ClassNode.EMPTY_ARRAY, code);
    }

    public ClosureMethodNode getOwner() {
        return owner;
    }

    public void setOwner(ClosureMethodNode owner) {
        this.owner = owner;
    }

    public List<Dependent> getDependentMethods() {
        return dependentMethods;
    }

    public void setDependentMethods(List<Dependent> dependentMethods) {
        this.dependentMethods = dependentMethods;
    }

    public void createDependentMethods(final ClassNode newType) {
        if (!hasDefaultValue())
            return;

        ArrayList<Dependent> results = new ArrayList<Dependent> ();

        Parameter[] parameters = getParameters();
        ArrayList<Integer> mutable = new ArrayList<Integer> ();
        for (int i = parameters.length-1; i >= 0; i--) {
            if (parameters [i].hasInitialExpression()) {
                mutable.add(i);
            }
        }

        for (int k = 0; k != mutable.size(); k++) {
            Parameter[] newParams = new Parameter[parameters.length - k - 1];
            ArgumentListExpression args = new ArgumentListExpression();
            for (int i = 0, l = 0; i != parameters.length; i++) {
                int mk = mutable.get(k);
                if (!parameters [i].hasInitialExpression() || i < mk) {
                    newParams [l++] = parameters [i];
                    args.addExpression(new VariableExpression(parameters[i]));
                }
                else {
                    args.addExpression(parameters[i].getInitialExpression());
                }
            }

            MethodCallExpression call = new MethodCallExpression(VariableExpression.THIS_EXPRESSION, getName(), args);
            call.setSourcePosition(this);

            ExpressionStatement code = new ExpressionStatement(call);
            code.setSourcePosition(call);

            Dependent _doCallMethodDef = new Dependent(
                    this,
                    getName(),
                    ACC_PUBLIC,
                    ClassHelper.OBJECT_TYPE,
                    newParams,
                    code
                    ){
            };

            newType.addMethod(_doCallMethodDef);
            results.add(_doCallMethodDef);
        }

        setDependentMethods(results);
        for (Parameter parameter : parameters) {
            parameter.setInitialExpression(null);
        }
    }

    public boolean checkOveride(MethodNode baseMethod, ClassNode baseType) {
        class Mutation {
            final Parameter p;
            final ClassNode t;

            public Mutation(ClassNode t, Parameter p) {
                this.t = t;
                this.p = p;
            }

            void mutate () {
                p.setType(t);
            }
        }

        List<Mutation> mutations = null;

        Parameter[] baseMethodParameters = baseMethod.getParameters();
        Parameter[] closureParameters = getParameters();

        boolean match = true;
        if (closureParameters.length == baseMethodParameters.length) {
            for (int i = 0; i < closureParameters.length; i++) {
                Parameter closureParameter = closureParameters[i];
                Parameter missingMethodParameter = baseMethodParameters[i];

                ClassNode parameterType = missingMethodParameter.getType();
                parameterType = TypeUtil.getSubstitutedType(parameterType, baseType.redirect(), baseType);
                if (!TypeUtil.isAssignableFrom(parameterType, closureParameter.getType())) {
                    if (TypeUtil.isAssignableFrom(closureParameter.getType(), parameterType)) {
                        if (mutations == null)
                            mutations = new LinkedList<Mutation>();
                        mutations.add(new Mutation(parameterType, closureParameter));
                        continue;
                    }
                    match = false;
                    break;
                }
            }

            if (match) {
                if (mutations != null)
                    for (Mutation mutation : mutations) {
                        mutation.mutate();
                    }
                ClassNode returnType = TypeUtil.getSubstitutedType(baseMethod.getReturnType(), baseType.redirect(), baseType);
                setReturnType(returnType);
                return true;
            }
        }

        if (dependentMethods != null) {
            for (Dependent dependentMethod : dependentMethods) {
                if(dependentMethod.checkOveride(baseMethod,  baseType))
                    return true;
            }
        }

        return false;
    }

    public static class Dependent extends ClosureMethodNode {
        private ClosureMethodNode master;

        public Dependent(ClosureMethodNode master, String name, int modifiers, ClassNode returnType, Parameter[] parameters, Statement code) {
            super(name, modifiers, returnType, parameters, code);
            this.master = master;
        }

        public ClosureMethodNode getMaster() {
            return master;
        }

        public String getTypeDescriptor() {
            StringBuilder buf = new StringBuilder(master.getName().length()+20);
            buf.append(master.getReturnType().getName());
            buf.append(' ');
            buf.append(master.getName());
            buf.append("(");
            for (int i = 0; i < getParameters().length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                Parameter param = getParameters()[i];
                buf.append(param.getType().getName());
            }
            buf.append(")");
            return buf.toString();
        }

        @Override
        public ClassNode getReturnType() {
            return master.getReturnType();
        }

        @Override
        public void setReturnType(ClassNode type) {
            if (master != null)
                master.setReturnType(type);
            else
                super.setReturnType(type);
        }
    }
}
