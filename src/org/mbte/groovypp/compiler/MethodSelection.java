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

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.util.FastArray;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.codehaus.groovy.ast.ClassHelper.*;

public class MethodSelection {

    private static final int
            OBJECT_SHIFT = 23, INTERFACE_SHIFT = 0,
            PRIMITIVE_SHIFT = 21, VARGS_SHIFT = 44;

    private static final ClassNode[] PRIMITIVES = {
            byte_TYPE, Byte_TYPE, short_TYPE, Short_TYPE,
            int_TYPE, Integer_TYPE, long_TYPE, Long_TYPE,
            BigInteger_TYPE, float_TYPE, Float_TYPE,
            double_TYPE, Double_TYPE, BigDecimal_TYPE,
            make(Number.class), OBJECT_TYPE
    };

    private static final int[][] PRIMITIVE_DISTANCE_TABLE = {
            //              byte    Byte    short   Short   int     Integer     long    Long    BigInteger  float   Float   double  Double  BigDecimal, Number, Object
            /* byte*/{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,},
            /*Byte*/{1, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,},
            /*short*/{14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,},
            /*Short*/{14, 15, 1, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,},
            /*int*/{14, 15, 12, 13, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,},
            /*Integer*/{14, 15, 12, 13, 1, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,},
            /*long*/{14, 15, 12, 13, 10, 11, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,},
            /*Long*/{14, 15, 12, 13, 10, 11, 1, 0, 2, 3, 4, 5, 6, 7, 8, 9,},
            /*BigInteger*/{14, 15, 12, 13, 10, 11, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7,},
            /*float*/{14, 15, 12, 13, 10, 11, 8, 9, 7, 0, 1, 2, 3, 4, 5, 6,},
            /*Float*/{14, 15, 12, 13, 10, 11, 8, 9, 7, 1, 0, 2, 3, 4, 5, 6,},
            /*double*/{14, 15, 12, 13, 10, 11, 8, 9, 7, 5, 6, 0, 1, 2, 3, 4,},
            /*Double*/{14, 15, 12, 13, 10, 11, 8, 9, 7, 5, 6, 1, 0, 2, 3, 4,},
            /*BigDecimal*/{14, 15, 12, 13, 10, 11, 8, 9, 7, 5, 6, 3, 4, 0, 1, 2,},
            /*Numer*/{14, 15, 12, 13, 10, 11, 8, 9, 7, 5, 6, 3, 4, 2, 0, 1,},
            /*Object*/{14, 15, 12, 13, 10, 11, 8, 9, 7, 5, 6, 3, 4, 2, 1, 0,},
    };
    private static final ClassNode[] ARRAY_WITH_NULL = {null};

    private static int getPrimitiveIndex(ClassNode c) {
        for (byte i = 0; i < PRIMITIVES.length; i++) {
            if (PRIMITIVES[i].equals(c)) return i;
        }
        return -1;
    }

    private static int getPrimitiveDistance(ClassNode from, ClassNode to) {
        // we know here that from!=to, so a distance of 0 is never valid
        // get primitive type indexes
        int fromIndex = getPrimitiveIndex(from);
        int toIndex = getPrimitiveIndex(to);
        if (fromIndex == -1 || toIndex == -1) return -1;
        return PRIMITIVE_DISTANCE_TABLE[toIndex][fromIndex];
    }

    private static int getMaximumInterfaceDistance(ClassNode c, ClassNode interfaceClass) {
        // -1 means a mismatch
        if (c == null) return -1;
        // 0 means a direct match
        if (c.redirect() == interfaceClass) return 0;
        ClassNode[] interfaces = c.getInterfaces();
        int max = -1;
        for (ClassNode anInterface : interfaces) {
            int sub = getMaximumInterfaceDistance(anInterface, interfaceClass);
            // we need to keep the -1 to track the mismatch, a +1
            // by any means could let it look like a direct match
            // we want to add one, because there is an interface between
            // the interface we search for and the interface we are in.
            if (sub != -1) sub++;
            // we are interested in the longest path only
            max = Math.max(max, sub);
        }
        // we do not add one for super classes, only for interfaces
        int superClassMax = getMaximumInterfaceDistance(c.getSuperClass(), interfaceClass);
        return Math.max(max, superClassMax);
    }

    public static Object chooseMethod(String methodName, Object methodOrList, ClassNode type, ClassNode[] arguments, ClassNode contextClass) {
        if (methodOrList instanceof MethodNode) {
            final MethodNode mn = (MethodNode) methodOrList;
            if (isValidMethod(mn.getParameters(), arguments, type, mn.getDeclaringClass())) {
                return methodOrList;
            }
            return null;
        }

        if (methodOrList == null) return null;
        FastArray methods = ((FastArray) methodOrList).copy();
        int methodCount = methods.size();
        if (methodCount > 1 && contextClass != null) {
            for (int i = 0; i < methodCount; i++) {
                final MethodNode methodNode = (MethodNode) methods.get(i);
                if (methodNode == null || !AccessibilityCheck.isAccessible(methodNode.getModifiers(), methodNode.getDeclaringClass(), contextClass, null)) {
                    methodCount--;
                    methods.remove(i--);
                }
            }
        }
        methodCount = methods.size();
        
        if (methodCount <= 0) {
            return null;
        } else if (methodCount == 1) {
            MethodNode method = (MethodNode) methods.get(0);
            if (isValidMethod(method.getParameters(), arguments, type, method.getDeclaringClass())) {
                return method;
            }
            return null;
        }
        Object answer;
        if (arguments == null || arguments.length == 0) {
            answer = chooseEmptyMethodParams(methods);
        } else if (arguments.length == 1 && arguments[0] == null) {
            answer = chooseMostGeneralMethodWith1NullParam(methods);
        } else {
            List<MethodNode> matchingMethods = new ArrayList<MethodNode>();

            final int len = methods.size;
            Object data[] = methods.getArray();
            boolean isValidExactFound = false;
            for (int i = 0; i != len; ++i) {
                MethodNode method = (MethodNode) data[i];

                if (isValidMethod(method.getParameters(), arguments, type, method.getDeclaringClass())) {
                    if (isValidExactMethod(arguments, method.getParameters(), type, method.getDeclaringClass())) {
                        if (!isValidExactFound) matchingMethods.clear();
                        isValidExactFound = true;
                    } else {
                        if (isValidExactFound) continue;
                    }
                    matchingMethods.add(method);
                }
            }

            if (matchingMethods.size() == 0) {
                return null;
            } else if (matchingMethods.size() == 1) {
                return matchingMethods.get(0);
            }
            return chooseMostSpecificParams(methodName, matchingMethods, arguments);
        }
        if (answer != null) {
            return answer;
        }
//        throw new MethodSelectionException(methodName, methods, arguments);
        return null;
    }

    private static Object chooseMostSpecificParams(String name, List<MethodNode> matchingMethods, ClassNode[] arguments) {
        long matchesDistance = -1;
        int matchesIndirect = -1;
        List<MethodNode> matches = new ArrayList<MethodNode>();
        for (Iterator<MethodNode> iter = matchingMethods.iterator(); iter.hasNext();) {
            MethodNode method = iter.next();
            Parameter[] paramTypes = method.getParameters();
            long dist = calculateParameterDistance(arguments, paramTypes);
            int indirectCount = getIndirectlyAssignableParamsCount(paramTypes, arguments);
            if (matches.size() == 0) {
                matches.add(method);
                matchesDistance = dist;
                matchesIndirect = indirectCount;
            } else if (indirectCount < matchesIndirect || (indirectCount == matchesIndirect && dist < matchesDistance)) {
                matchesDistance = dist;
                matchesIndirect = indirectCount;
                matches.clear();
                matches.add(method);
            } else if (indirectCount == matchesIndirect && dist == matchesDistance) {
                matches.add(method);
            }

        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        if (matches.size() == 0) {
            return null;
        }

        return null;
    }

    private static int getIndirectlyAssignableParamsCount(Parameter[] params, ClassNode[] args) {
        int res = 0;
        for (int i = 0; i < params.length - 1; i++) {
            if (!isAssignableDirectly(params[i].getType(), args[i])) res++;
        }
        final ClassNode lastParamType = params[params.length - 1].getType();
        if (params.length == args.length && isAssignableOrInference(lastParamType,
                args[params.length -1])) {
            if (!isAssignableDirectly(lastParamType, args[params.length -1])) res++;
        } else if (args.length > params.length) {
            ClassNode last;
            if (lastParamType.isArray()) {
                last = lastParamType.getComponentType();
            } else {
                last = TypeUtil.getSubstitutedType(ClassHelper.LIST_TYPE.getGenericsTypes()[0].getType(),
                        ClassHelper.LIST_TYPE, lastParamType);
            }
            for (int i =  params.length - 1; i < args.length;  i++) {
                if (!isAssignableDirectly(last, args[i])) {
                    res++;
                    break;
                }
            }
        }
        return res;
    }

    private static boolean isAssignableDirectly(ClassNode param, ClassNode arg) {
        if (arg == null) return true;
        if (param == ClassHelper.boolean_TYPE) {
            return arg == ClassHelper.boolean_TYPE;
        }

        if (getPrimitiveIndex(param) >= 0 && getPrimitiveIndex(arg) >= 0) return true;

        // We cannot argue about closure assignability here.
        if (arg.implementsInterface(TypeUtil.TCLOSURE) || arg.redirect() == TypeUtil.TCLOSURE_NULL) {
            return true;
        }

        if (arg.implementsInterface(TypeUtil.TLIST)) arg = TypeUtil.ARRAY_LIST_TYPE;
        if (arg.implementsInterface(TypeUtil.TMAP)) arg = TypeUtil.LINKED_HASH_MAP_TYPE;

        return TypeUtil.isDirectlyAssignableFrom(TypeUtil.wrapSafely(param), TypeUtil.wrapSafely(arg));
    }

    private static long calculateParameterDistance(ClassNode argument, ClassNode parameter) {
        /**
         * note: when shifting with 32 bit, you should only shift on a long. If you do
         *       that with an int, then i==(i<<32), which means you loose the shift
         *       information
         */

        if (argument == null || parameter == null || parameter.equals(argument)) return 0;

        if (argument.implementsInterface(TypeUtil.TLIST)) argument = TypeUtil.ARRAY_LIST_TYPE;
        if (argument.implementsInterface(TypeUtil.TMAP)) argument = TypeUtil.LINKED_HASH_MAP_TYPE;

        if (parameter.isInterface()) {
            return getMaximumInterfaceDistance(argument, parameter) << INTERFACE_SHIFT;
        }

        long objectDistance = 0;
        if (argument != null) {
            long pd = getPrimitiveDistance(parameter, argument);
            if (pd != -1) return pd << PRIMITIVE_SHIFT;

            // add one to dist to be sure interfaces are preferred
            objectDistance += PRIMITIVES.length + 1;
            ClassNode clazz = TypeUtil.wrapSafely(argument);
            while (clazz != null) {
                if (clazz.equals(parameter)) break;
                if (clazz.equals(GSTRING_TYPE) && parameter.equals(STRING_TYPE)) {
                    objectDistance += 2;
                    break;
                }
                clazz = clazz.getSuperClass();
                objectDistance += 3;
            }
        } else {
            // choose the distance to Object if a parameter is null
            // this will mean that Object is preferred over a more
            // specific type
            // remove one to dist to be sure Object is preferred
            objectDistance--;
            ClassNode clazz = parameter;
            if (isPrimitiveType(clazz)) {
                objectDistance += 2;
            } else {
                while (!clazz.equals(OBJECT_TYPE)) {
                    clazz = clazz.getSuperClass();
                    objectDistance += 2;
                }
            }
        }
        return objectDistance << OBJECT_SHIFT;
    }

    public static long calculateParameterDistance(ClassNode[] arguments, Parameter[] parameters) {
        if (parameters.length == 0) return 0;

        long ret = 0;
        int noVargsLength = parameters.length - 1;

        // if the number of parameters does not match we have
        // a vargs usage
        //
        // case A: arguments.length<parameters.length
        //
        //         In this case arguments.length is always equal to
        //         noVargsLength because only the last parameter
        //         might be a optional vargs parameter
        //
        //         VArgs penalty: 1l
        //
        // case B: arguments.length>parameters.length
        //
        //         In this case all arguments with a index bigger than
        //         paramMinus1 are part of the vargs, so a
        //         distance calculation needs to be done against
        //         parameters[noVargsLength].getComponentType()
        //
        //         VArgs penalty: 2l+arguments.length-parameters.length
        //
        // case C: arguments.length==parameters.length &&
        //         isAssignableFrom( parameters[noVargsLength],
        //                           arguments[noVargsLength] )
        //
        //         In this case we have no vargs, so calculate directly
        //
        //         VArgs penalty: 0l
        //
        // case D: arguments.length==parameters.length &&
        //         !isAssignableFrom( parameters[noVargsLength],
        //                            arguments[noVargsLength] )
        //
        //         In this case we have a vargs case again, we need
        //         to calculate arguments[noVargsLength] against
        //         parameters[noVargsLength].getComponentType
        //
        //         VArgs penalty: 2l
        //
        //         This gives: VArgs_penalty(C)<VArgs_penalty(A)
        //                     VArgs_penalty(A)<VArgs_penalty(D)
        //                     VArgs_penalty(D)<VArgs_penalty(B)

        /**
         * In general we want to match the signature that allows us to use
         * as less arguments for the vargs part as possible. That means the
         * longer signature usually wins if both signatures are vargs, while
         * vargs looses always against a signature without vargs.
         *
         *  A vs B :
         *      def foo(Object[] a) {1}     -> case B
         *      def foo(a,b,Object[] c) {2} -> case A
         *      assert foo(new Object(),new Object()) == 2
         *  --> A preferred over B
         *
         *  A vs C :
         *      def foo(Object[] a) {1}     -> case B
         *      def foo(a,b)        {2}     -> case C
         *      assert foo(new Object(),new Object()) == 2
         *  --> C preferred over A
         *
         *  A vs D :
         *      def foo(Object[] a) {1}     -> case D
         *      def foo(a,Object[] b) {2}   -> case A
         *      assert foo(new Object()) == 2
         *  --> A preferred over D
         *
         *  This gives C<A<B,D
         *
         *  B vs C :
         *      def foo(Object[] a) {1}     -> case B
         *      def foo(a,b) {2}            -> case C
         *      assert foo(new Object(),new Object()) == 2
         *  --> C preferred over B, matches C<A<B,D
         *
         *  B vs D :
         *      def foo(Object[] a)   {1}   -> case B
         *      def foo(a,Object[] b) {2}   -> case D
         *      assert foo(new Object(),new Object()) == 2
         *  --> D preferred over B
         *
         *  This gives C<A<D<B
         */

        // first we calculate all arguments, that are for sure not part
        // of vargs.  Since the minimum for arguments is noVargsLength
        // we can safely iterate to this point
        for (int i = 0; i < noVargsLength; i++) {
            ret += calculateParameterDistance(arguments[i], parameters[i].getType());
        }

        if (arguments.length == parameters.length) {
            // case C&D, we use baseType to calculate and set it
            // to the value we need according to case C and D
            ClassNode baseType = parameters[noVargsLength].getType(); // case C
            if (!isAssignableOrInference(parameters[noVargsLength].getType(), arguments[noVargsLength])) {
                baseType = baseType.getComponentType(); // case D
                ret += 2l << VARGS_SHIFT; // penalty for vargs
            }
            ret += calculateParameterDistance(arguments[noVargsLength], baseType);
        } else if (arguments.length > parameters.length) {
            // case B
            // we give our a vargs penalty for each exceeding argument and iterate
            // by using parameters[noVargsLength].getComponentType()
            ret += (2l + arguments.length - parameters.length) << VARGS_SHIFT; // penalty for vargs
            ClassNode vargsType = (parameters[noVargsLength].getType().getComponentType());
            for (int i = noVargsLength; i < arguments.length; i++) {
                ret += calculateParameterDistance(arguments[i], vargsType);
            }
        } else {
            // case A
            // we give a penalty for vargs, since we have no direct
            // match for the last argument
            ret += 1l << VARGS_SHIFT;
        }

        return ret;
    }

    /**
     * @param methods the methods to choose from
     * @return the method with 1 parameter which takes the most general type of
     *         object (e.g. Object)
     */
    public static Object chooseEmptyMethodParams(FastArray methods) {
        Object vargsMethod = null;
        final int len = methods.size();
        final Object[] data = methods.getArray();
        for (int i = 0; i != len; ++i) {
            MethodNode method = (MethodNode) data[i];
            final Parameter pt[] = method.getParameters();
            int paramLength = pt.length;
            if (paramLength == 0) {
                return method;
            } else if (paramLength == 1 && isVargsMethodNoParams(pt)) {
                vargsMethod = method;
            }
        }
        return vargsMethod;
    }

    /**
     * @param methods the methods to choose from
     * @return the method with 1 parameter which takes the most general type of
     *         object (e.g. Object) ignoring primitve types
     */
    public static Object chooseMostGeneralMethodWith1NullParam(FastArray methods) {
        // let's look for methods with 1 argument which matches the type of the
        // arguments
        ClassNode closestClass = null;
        ClassNode closestVargsClass = null;
        Object answer = null;
        int closestDist = -1;
        final int len = methods.size();
        for (int i = 0; i != len; ++i) {
            final Object[] data = methods.getArray();
            MethodNode method = (MethodNode) data[i];
            final Parameter[] pt = method.getParameters();
            int paramLength = pt.length;
            if (paramLength == 0 || paramLength > 2) continue;

            ClassNode theType = pt[0].getType();
            if (isPrimitiveType(theType)) continue;

            if (paramLength == 2) {
                if (!isVargsMethod(pt, ARRAY_WITH_NULL)) continue;
                if (closestClass == null) {
                    closestVargsClass = pt[1].getType();
                    closestClass = theType;
                    answer = method;
                } else if (closestClass.equals(theType)) {
                    if (closestVargsClass == null) continue;
                    ClassNode newVargsClass = pt[1].getType();
                    if (closestVargsClass == null || isAssignableOrInference(newVargsClass, closestVargsClass)) {
                        closestVargsClass = newVargsClass;
                        answer = method;
                    }
                } else if (isAssignableOrInference(theType, closestClass)) {
                    closestVargsClass = pt[1].getType();
                    closestClass = theType;
                    answer = method;
                }
            } else {
                if (closestClass == null || isAssignableOrInference(theType, closestClass)) {
                    closestVargsClass = null;
                    closestClass = theType;
                    answer = method;
                    closestDist = -1;
                } else {
                    // closestClass and theType are not in a subtype relation, we need
                    // to check the distance to Object
                    if (closestDist == -1) closestDist = getSuperClassDistance(closestClass);
                    int newDist = getSuperClassDistance(theType);
                    if (newDist == closestDist) answer = null;
                    if (newDist < closestDist) {
                        closestDist = newDist;
                        closestVargsClass = null;
                        closestClass = theType;
                        answer = method;
                    }
                }
            }
        }
        return answer;
    }

    private static int getSuperClassDistance(ClassNode klazz) {
        int distance = 0;
        for (; klazz != null; klazz = klazz.getSuperClass()) {
            distance++;
        }
        return distance;
    }

    private static boolean isVargsMethod(Parameter pt[], ClassNode[] arguments) {
        if (pt.length == 0 || !pt[pt.length - 1].getType().isArray())
            return false;

        final int lenMinus1 = pt.length - 1;
        // -1 because the varg part is optional
        if (lenMinus1 == arguments.length) return true;
        if (lenMinus1 > arguments.length) return false;
        if (arguments.length > pt.length) return true;

        // only case left is arguments.length == parameterTypes.length
        ClassNode last = arguments[arguments.length - 1];
        return last == null || !last.equals(pt[lenMinus1].getType());

    }

    public static boolean isVargsMethodNoParams(Parameter pt[]) {
        return (pt.length == 1 && pt[pt.length - 1].getType().isArray());
    }

    private static boolean isValidMethod(Parameter[] pt, ClassNode[] arguments, ClassNode accessType, ClassNode declaringClass) {
        if (arguments == null) return true;

        final int size = arguments.length;
        final int paramMinus1 = pt.length - 1;

        if (isValidExactMethod(arguments, pt, accessType, declaringClass)) {
            return true;
        }

        boolean isVargsMethod = pt.length != 0 && (pt[pt.length - 1].getType().isArray() ||
                TypeUtil.isDirectlyAssignableFrom(ClassHelper.LIST_TYPE, pt[pt.length - 1].getType()));
        
        if (isVargsMethod && size >= paramMinus1)
            return isValidVarargsMethod(arguments, size, pt, paramMinus1, accessType, declaringClass);

        return false;
    }

    private static boolean isValidExactMethod(ClassNode[] arguments, Parameter[] pt, ClassNode accessType, ClassNode declaringClass) {
        if (pt.length != arguments.length) return false;
        // lets check the parameter types match
        int size = pt.length;
        for (int i = 0; i < size; i++) {
            ClassNode t = pt[i].getType();
            if (accessType != null) {
                t = TypeUtil.getSubstitutedType(t, declaringClass, accessType);
            }
            if (!isAssignableOrInference(t, arguments[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean testComponentAssignable(ClassNode toTestAgainst, ClassNode toTest) {
        ClassNode component = toTest.getComponentType();
        return component != null && isAssignableOrInference(toTestAgainst, component);
    }

    private static boolean isValidVarargsMethod(ClassNode[] arguments,
                                                int size,
                                                Parameter[] pt,
                                                int paramMinus1,
                                                ClassNode accessType,
                                                ClassNode declaringClass) {
        // first check normal number of parameters
        for (int i = 0; i < paramMinus1; i++) {
            ClassNode t = pt[i].getType();
            if (accessType != null) {
                t = TypeUtil.getSubstitutedType(t, declaringClass, accessType);
            }
            if (isAssignableOrInference(t, arguments[i])) continue;
            return false;
        }

        // check direct match
        ClassNode varg = pt[paramMinus1].getType();
        ClassNode componentType;
        if (varg.isArray()) {
            componentType = varg.getComponentType();
        } else {
            // List type
            final GenericsType[] generics = LIST_TYPE.getGenericsTypes();
            if (generics == null) return false;
            componentType = TypeUtil.getSubstitutedType(generics[0].getType(), LIST_TYPE, varg);
        }
        if (accessType != null) {
            componentType = TypeUtil.getSubstitutedType(componentType, declaringClass, accessType);
        }

        if (size == pt.length &&
                (TypeUtil.isDirectlyAssignableFrom(varg, arguments[paramMinus1]) ||
                        testComponentAssignable(componentType, arguments[paramMinus1]))) {
            return true;
        }

        // check varged
        for (int i = paramMinus1; i < size; i++) {
            if (isAssignableOrInference(componentType, arguments[i])) continue;
            return false;
        }
        return true;
    }

    public static MethodNode findPublicMethodInClass(ClassNode classToTransformFrom, String methodName, ClassNode[] args) {
        Object methodOrList = ClassNodeCache.getMethods(classToTransformFrom, methodName);
        if (methodOrList instanceof MethodNode) {
            if (!((MethodNode)methodOrList).isPublic()) return null;
        } else {
            final FastArray array = (FastArray) methodOrList;
            for (int i = 0; i < array.size(); i++) {
                final MethodNode method = (MethodNode) array.get(i);
                if (!method.isPublic()) array.remove(i);
            }
        }
        final Object selected = chooseMethod(methodName, methodOrList, classToTransformFrom, args, null);
        return selected instanceof MethodNode ? (MethodNode)selected : null;
    }

    private static boolean isAssignableOrInference(ClassNode classToTransformTo, ClassNode classToTransformFrom) {
        if (TypeUtil.isAssignableFrom(classToTransformTo, classToTransformFrom))
            return true;

        if(classToTransformFrom.equals(TypeUtil.TCLOSURE_NULL)) {
            if (classToTransformTo.equals(ClassHelper.CLOSURE_TYPE)) {
                return true;
            }
            else {
                List<MethodNode> one = ClosureUtil.isOneMethodAbstract(classToTransformTo);
                if (one != null) {

                    MethodNode missing = one.get(0);
                    Parameter[] missingMethodParameters = missing.getParameters();
                    List<MethodNode> methods = classToTransformFrom.getGenericsTypes()[0].getType().getDeclaredMethods("doCall");

                    for (MethodNode method : methods) {
                        Parameter[] closureParameters = method.getParameters();

                        if (closureParameters.length != missingMethodParameters.length)
                            continue;

                        return true;
                    }
                }
                return false;
            }
        }

        return false;
    }
}

