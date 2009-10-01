package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.mbte.groovypp.compiler.TypeUtil;

import java.util.IdentityHashMap;
import java.util.Map;

public class LocalVarInferenceTypes extends BytecodeLabelInfo {

    IdentityHashMap<Variable, ClassNode> defVars;
    private boolean visited;

    public void add(VariableExpression ve, ClassNode type) {
        if (defVars == null)
            defVars = new IdentityHashMap<Variable, ClassNode>();

        defVars.put(ve.getAccessedVariable(), type);
    }

    public ClassNode get(VariableExpression ve) {
        return defVars == null ? ClassHelper.OBJECT_TYPE : defVars.get(ve.getAccessedVariable());
    }

    public ClassNode get(Variable ve) {
        return defVars == null ? ClassHelper.OBJECT_TYPE : defVars.get(ve);
    }

    public void addWeak(VariableExpression ve, ClassNode type) {
        final ClassNode oldType = defVars.get(ve.getAccessedVariable());
        if (oldType == null) {
            defVars.put(ve.getAccessedVariable(), type);
        } else {
            if (oldType != TypeUtil.NULL_TYPE) {
                if (TypeUtil.isDirectlyAssignableFrom(oldType, type))
                    defVars.put(ve.getAccessedVariable(), type);
            }
        }
    }


    public void jumpFrom(LocalVarInferenceTypes cur) {
        if (visited) {
            // jump back - we need to check for conflict
            if (cur.defVars != null)
                for (Map.Entry<Variable, ClassNode> e : cur.defVars.entrySet()) {
                    final ClassNode oldType = defVars.get(e.getKey());
                    if (oldType != null) {
                        if (!TypeUtil.isDirectlyAssignableFrom(oldType, e.getValue()))
                            throw new RuntimeException("Illegal type inference for variable '" + e.getKey().getName() + "'");
                    }
                }
            else
                throw new RuntimeException("Shouldn't happen");
        } else {
            if (defVars == null) {
                // we are 1st time here - just init
                if (cur.defVars != null)
                    defVars = (IdentityHashMap<Variable, ClassNode>) cur.defVars.clone();
                else
                    defVars = new IdentityHashMap<Variable, ClassNode>();
            } else {
                // we were here already, so we need to merge
                if (cur.defVars != null)
                    for (Map.Entry<Variable, ClassNode> e : cur.defVars.entrySet()) {
                        final ClassNode oldType = defVars.get(e.getKey());
                        if (oldType != null)
                            defVars.put(e.getKey(), ClassHelper.getWrapper(TypeUtil.commonType(e.getValue(), oldType)));
                    }
            }
        }
    }

    public void comeFrom(LocalVarInferenceTypes cur) {
        jumpFrom(cur);
        visited = true;
    }
}
