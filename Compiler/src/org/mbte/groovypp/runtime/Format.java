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

package org.mbte.groovypp.runtime;

import groovy.lang.Range;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import java.util.Collection;
import java.util.Map;

/**
 * Utilities to format objects to Strings
 */
public class Format {
    public static String toString(Object arguments) {
        return toString(arguments, new StringBuilder()).toString();
    }

    public static StringBuilder toString(Object arguments, StringBuilder sb) {
        return format(arguments, sb);
    }

    public static StringBuilder toArrayString(Object[] arguments, StringBuilder sb) {
        if (arguments == null) {
            return sb.append("null");
        }
        sb.append("[");
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            format(arguments[i], sb);
        }
        return sb.append("]");
    }

    public static StringBuilder toListString(Collection arg, StringBuilder sb) {
        boolean first = true;
        sb.append("[");
        for (Object item : arg) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            if (item == arg) {
                sb.append("(this Collection)");
            } else {
                format(item, sb);
            }
        }
        return sb.append("]");
    }

    protected static StringBuilder format(Object arguments, StringBuilder sb) {
        if (arguments == null) {
            return sb.append("null");
        }
        if (arguments.getClass().isArray()) {
            if (arguments instanceof char[]) {
                return sb.append(new String((char[]) arguments));
            }
            if (arguments.getClass().getComponentType().isPrimitive())
                return format(DefaultTypeTransformation.asCollection(arguments), sb);
            else
                return toArrayString((Object[])arguments, sb);
        }
        if (arguments instanceof Range) {
            Range range = (Range) arguments;
            return sb.append(range.toString());
        }
        if (arguments instanceof Collection) {
            return toListString((Collection) arguments, sb);
        }
        if (arguments instanceof Map) {
            return toMapString((Map) arguments, sb);
        }
        return sb.append(arguments);
    }

    private static StringBuilder toMapString(Map map, StringBuilder sb) {
        if (map.isEmpty()) {
            return sb.append("[:]");
        }
        boolean first = true;
        sb.append("[");
        for (Object o : map.entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            Map.Entry entry = (Map.Entry) o;
            format(entry.getKey(), sb);
            sb.append(":");
            if (entry.getValue() == map) {
                sb.append("(this Map)");
            } else {
                format(entry.getValue(), sb);
            }
        }
        return sb.append("]");
    }
}
