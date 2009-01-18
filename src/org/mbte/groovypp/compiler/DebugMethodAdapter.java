package org.mbte.groovypp.compiler;

import org.codehaus.groovy.reflection.CachedField;
import org.codehaus.groovy.reflection.ReflectionCache;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

class DebugMethodAdapter{

    private static HashMap<Integer,String> map = new HashMap<Integer,String>();

    static {
        final CachedField[] cachedFields = ReflectionCache.getCachedClass(Opcodes.class).getFields();
        for (int i = 0; i < cachedFields.length; i++) {
            CachedField cachedField = cachedFields[i];
            final Integer v = (Integer)cachedField.getProperty(null);
            map.put (v, cachedField.getName());
        }
    }

    static MethodVisitor create(final MethodVisitor mv) {
        final Set codes = new HashSet();
        codes.addAll(Arrays.asList("visitVarInsn", "visitMethodInsn", "visitInsn", "visitJumpInsn", "visitTypeInsn", "visitFieldInsn"));
        return (MethodVisitor) Proxy.newProxyInstance(DebugMethodAdapter.class.getClassLoader(), new Class[]{MethodVisitor.class}, new InvocationHandler(){
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getDeclaringClass() != Class.class) {
                    System.out.print(method.getName() + "(");
                    for (int i = 0; i < args.length; i++) {
                        Object arg = args[i];
                        if (i == 0 && codes.contains(method.getName())) {
                          System.out.print(map.get((Integer)args[i]) + ", ");
                        }
                        else {
                          if (i == 0 && method.getName().equals("visitLdcInsn"))
                             System.out.print("(" + args[i].getClass().getName() + ")" + args[i] + ", ");
                          else
                             System.out.print(args[i] + ", ");
                        }
                    }
                    System.out.println(")");
                }
                return method.invoke(mv, args);
            }
        });
    }
}
