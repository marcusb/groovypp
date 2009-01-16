package org.mbte.groovypp.compiler;

import org.objectweb.asm.*;
import org.codehaus.groovy.classgen.BytecodeInstruction;

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class StoredBytecodeInstruction extends BytecodeInstruction {

    private final List operations = new ArrayList();

    public void visit(MethodVisitor mv) {
        for (Iterator it = operations.iterator(); it.hasNext(); ) {
            Method m = (Method) it.next();
            Object [] args = (Object[]) it.next();
            try {
                m.invoke(mv, args);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
        }
        operations.clear();
    }

    public MethodVisitor createStorage() {
        return (MethodVisitor) Proxy.newProxyInstance(StoredBytecodeInstruction.class.getClassLoader(), new Class[]{MethodVisitor.class}, new InvocationHandler(){
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                operations.add(method);
                operations.add(args);
                return null;
            }
        });
    }
}