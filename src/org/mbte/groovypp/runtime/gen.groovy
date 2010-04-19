/*
 * Copyright (c) 2009-10. MBTE Sweden AB. All Rights reserved
 */

package org.mbte.groovypp.runtime

def numbers = [byte, short, int, long, float, double, char]

numbers.each { it ->
  println """
    public static boolean equals($it [] left, Object right) {
        if (left == right) return true;
        if (left == null || right == null) return false;"""

  numbers.each { jt ->
    println """
        if(right instanceof $jt []) {
          return equals0(left, ($jt [])right);
        } else"""
  }

  println """
       if(right instanceof boolean[]) {
          return equals0(left, (boolean [])right);
       } else
       if(right instanceof Object[]) {
          return equals0(left, (Object [])right);
       } else
       """

  println """
        return false;
    }"""

  numbers.each { jt ->
    println """
    private static boolean equals0($it [] left, $jt [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }"""

    println """
    public static boolean equals($it [] left, $jt [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }"""
  }

  println """
  private static boolean equals0($it [] left, boolean [] right) {
    int length = right.length;
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++)
        if ((left[i] != 0) != right[i])
            return false;

    return true;
  }

  public static boolean equals($it [] left, boolean [] right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }

  private static boolean equals0($it [] left, Object [] right) {
    int length = right.length;
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++) {
       Object v = right[i];
       if (v instanceof Number) {
          if( ((Number)v).${it.name=='char'?'int':it}Value () != left[i] )
            return false;
       } else
       if (v instanceof Character) {
          if( ((Character)v) != left[i] )
            return false;
       } else
       if (v instanceof Boolean) {
          if( ((Boolean)v) != (left[i] != 0))
            return false;
       } else
       return false;
    }

    return true;
  }

  public static boolean equals($it [] left, Object [] right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }

  private static boolean equals0($it [] left, List right) {
    int length = right.size();
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++) {
       Object v = right.get(i);
       if (v instanceof Number) {
          if( ((Number)v).${it.name=='char'?'int':it}Value () != left[i] )
            return false;
       } else
       if (v instanceof Character) {
          if( ((Character)v) != left[i] )
            return false;
       } else
       if (v instanceof Boolean) {
          if( ((Boolean)v) != (left[i] != 0))
            return false;
       } else
       return false;
    }

    return true;
  }

  public static boolean equals($it [] left, List right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }
  """
}