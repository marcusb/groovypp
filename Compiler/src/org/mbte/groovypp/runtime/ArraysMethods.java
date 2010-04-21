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

import java.util.Iterator;
import java.util.List;

import static org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation.compareArrayEqual;
import static org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation.primitiveArrayToList;

public class ArraysMethods {
    private static int normaliseIndex(int i, int size) {
        /*int temp = i;*/
        if (i < 0) {
            i += size;
        }
        /*if (i < 0) {
            throw new ArrayIndexOutOfBoundsException("Negative array index [" + temp + "] too large for array size " + size);
        }*/
        return i;
    }

    public static <T> T getAt (T [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static <T> void putAt (T [] self, int i, T v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static byte getAt(byte [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static  void putAt (byte [] self, int i, byte v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static short getAt(short [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static  void putAt (short [] self, int i, short v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static int getAt(int [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static  void putAt (int [] self, int i, int v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static char getAt(char [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static  void putAt (char [] self, int i, char v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static float getAt(float [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static  void putAt (float [] self, int i, float v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static double getAt(double [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static  void putAt (double [] self, int i, double v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static boolean getAt(boolean [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static  void putAt (boolean [] self, int i, boolean v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static long getAt(long [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static  void putAt (long [] self, int i, long v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static Iterator<Character> iterator (final char self []) {
        return new Iterator<Character> () {
            int count = 0;

            public boolean hasNext() {
                return count != self.length;
            }

            public Character next() {
                return self[count++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterator<Boolean> iterator (final boolean self []) {
        return new Iterator<Boolean> () {
            int count = 0;

            public boolean hasNext() {
                return count != self.length;
            }

            public Boolean next() {
                return self[count++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterator<Byte> iterator (final byte self []) {
        return new Iterator<Byte> () {
            int count = 0;

            public boolean hasNext() {
                return count != self.length;
            }

            public Byte next() {
                return self[count++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterator<Short> iterator (final short self []) {
        return new Iterator<Short> () {
            int count = 0;

            public boolean hasNext() {
                return count != self.length;
            }

            public Short next() {
                return self[count++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterator<Integer> iterator (final int self []) {
        return new Iterator<Integer> () {
            int count = 0;

            public boolean hasNext() {
                return count != self.length;
            }

            public Integer next() {
                return self[count++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterator<Long> iterator (final long self []) {
        return new Iterator<Long> () {
            int count = 0;

            public boolean hasNext() {
                return count != self.length;
            }

            public Long next() {
                return self[count++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterator<Float> iterator (final float self []) {
        return new Iterator<Float> () {
            int count = 0;

            public boolean hasNext() {
                return count != self.length;
            }

            public Float next() {
                return self[count++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterator<Double> iterator (final double self []) {
        return new Iterator<Double> () {
            int count = 0;

            public boolean hasNext() {
                return count != self.length;
            }

            public Double next() {
                return self[count++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static boolean equals(byte [] left, Object right) {
        if (left == right) return true;
        if (left == null || right == null) return false;

        if(right instanceof byte []) {
          return equals0(left, (byte [])right);
        } else

        if(right instanceof short []) {
          return equals0(left, (short [])right);
        } else

        if(right instanceof int []) {
          return equals0(left, (int [])right);
        } else

        if(right instanceof long []) {
          return equals0(left, (long [])right);
        } else

        if(right instanceof float []) {
          return equals0(left, (float [])right);
        } else

        if(right instanceof double []) {
          return equals0(left, (double [])right);
        } else

        if(right instanceof char []) {
          return equals0(left, (char [])right);
        } else

       if(right instanceof boolean[]) {
          return equals0(left, (boolean [])right);
       } else
       if(right instanceof Object[]) {
          return equals0(left, (Object [])right);
       } else


        return false;
    }

    private static boolean equals0(byte [] left, byte [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(byte [] left, byte [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(byte [] left, short [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(byte [] left, short [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(byte [] left, int [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(byte [] left, int [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(byte [] left, long [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(byte [] left, long [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(byte [] left, float [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(byte [] left, float [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(byte [] left, double [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(byte [] left, double [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(byte [] left, char [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(byte [] left, char [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

  private static boolean equals0(byte [] left, boolean [] right) {
    int length = right.length;
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++)
        if ((left[i] != 0) != right[i])
            return false;

    return true;
  }

  public static boolean equals(byte [] left, boolean [] right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }

  private static boolean equals0(byte [] left, Object [] right) {
    int length = right.length;
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++) {
       Object v = right[i];
       if (v instanceof Number) {
          if( ((Number)v).byteValue () != left[i] )
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

  public static boolean equals(byte [] left, Object [] right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }

  private static boolean equals0(byte [] left, List right) {
    int length = right.size();
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++) {
       Object v = right.get(i);
       if (v instanceof Number) {
          if( ((Number)v).byteValue () != left[i] )
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

  public static boolean equals(byte [] left, List right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }


    public static boolean equals(short [] left, Object right) {
        if (left == right) return true;
        if (left == null || right == null) return false;

        if(right instanceof byte []) {
          return equals0(left, (byte [])right);
        } else

        if(right instanceof short []) {
          return equals0(left, (short [])right);
        } else

        if(right instanceof int []) {
          return equals0(left, (int [])right);
        } else

        if(right instanceof long []) {
          return equals0(left, (long [])right);
        } else

        if(right instanceof float []) {
          return equals0(left, (float [])right);
        } else

        if(right instanceof double []) {
          return equals0(left, (double [])right);
        } else

        if(right instanceof char []) {
          return equals0(left, (char [])right);
        } else

       if(right instanceof boolean[]) {
          return equals0(left, (boolean [])right);
       } else
       if(right instanceof Object[]) {
          return equals0(left, (Object [])right);
       } else


        return false;
    }

    private static boolean equals0(short [] left, byte [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(short [] left, byte [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(short [] left, short [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(short [] left, short [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(short [] left, int [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(short [] left, int [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(short [] left, long [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(short [] left, long [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(short [] left, float [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(short [] left, float [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(short [] left, double [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(short [] left, double [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(short [] left, char [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(short [] left, char [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

  private static boolean equals0(short [] left, boolean [] right) {
    int length = right.length;
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++)
        if ((left[i] != 0) != right[i])
            return false;

    return true;
  }

  public static boolean equals(short [] left, boolean [] right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }

  private static boolean equals0(short [] left, Object [] right) {
    int length = right.length;
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++) {
       Object v = right[i];
       if (v instanceof Number) {
          if( ((Number)v).shortValue () != left[i] )
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

  public static boolean equals(short [] left, Object [] right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }

  private static boolean equals0(short [] left, List right) {
    int length = right.size();
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++) {
       Object v = right.get(i);
       if (v instanceof Number) {
          if( ((Number)v).shortValue () != left[i] )
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

  public static boolean equals(short [] left, List right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }


    public static boolean equals(int [] left, Object right) {
        if (left == right) return true;
        if (left == null || right == null) return false;

        if(right instanceof byte []) {
          return equals0(left, (byte [])right);
        } else

        if(right instanceof short []) {
          return equals0(left, (short [])right);
        } else

        if(right instanceof int []) {
          return equals0(left, (int [])right);
        } else

        if(right instanceof long []) {
          return equals0(left, (long [])right);
        } else

        if(right instanceof float []) {
          return equals0(left, (float [])right);
        } else

        if(right instanceof double []) {
          return equals0(left, (double [])right);
        } else

        if(right instanceof char []) {
          return equals0(left, (char [])right);
        } else

       if(right instanceof boolean[]) {
          return equals0(left, (boolean [])right);
       } else
       if(right instanceof Object[]) {
          return equals0(left, (Object [])right);
       } else


        return false;
    }

    private static boolean equals0(int [] left, byte [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(int [] left, byte [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(int [] left, short [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(int [] left, short [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(int [] left, int [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(int [] left, int [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(int [] left, long [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(int [] left, long [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(int [] left, float [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(int [] left, float [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(int [] left, double [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(int [] left, double [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(int [] left, char [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(int [] left, char [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

  private static boolean equals0(int [] left, boolean [] right) {
    int length = right.length;
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++)
        if ((left[i] != 0) != right[i])
            return false;

    return true;
  }

  public static boolean equals(int [] left, boolean [] right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }

  private static boolean equals0(int [] left, Object [] right) {
    int length = right.length;
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++) {
       Object v = right[i];
       if (v instanceof Number) {
          if( ((Number)v).intValue () != left[i] )
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

  public static boolean equals(int [] left, Object [] right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }

  private static boolean equals0(int [] left, List right) {
    int length = right.size();
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++) {
       Object v = right.get(i);
       if (v instanceof Number) {
          if( ((Number)v).intValue () != left[i] )
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

  public static boolean equals(int [] left, List right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }


    public static boolean equals(long [] left, Object right) {
        if (left == right) return true;
        if (left == null || right == null) return false;

        if(right instanceof byte []) {
          return equals0(left, (byte [])right);
        } else

        if(right instanceof short []) {
          return equals0(left, (short [])right);
        } else

        if(right instanceof int []) {
          return equals0(left, (int [])right);
        } else

        if(right instanceof long []) {
          return equals0(left, (long [])right);
        } else

        if(right instanceof float []) {
          return equals0(left, (float [])right);
        } else

        if(right instanceof double []) {
          return equals0(left, (double [])right);
        } else

        if(right instanceof char []) {
          return equals0(left, (char [])right);
        } else

       if(right instanceof boolean[]) {
          return equals0(left, (boolean [])right);
       } else
       if(right instanceof Object[]) {
          return equals0(left, (Object [])right);
       } else


        return false;
    }

    private static boolean equals0(long [] left, byte [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(long [] left, byte [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(long [] left, short [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(long [] left, short [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(long [] left, int [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(long [] left, int [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(long [] left, long [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(long [] left, long [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(long [] left, float [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(long [] left, float [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(long [] left, double [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(long [] left, double [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(long [] left, char [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(long [] left, char [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

  private static boolean equals0(long [] left, boolean [] right) {
    int length = right.length;
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++)
        if ((left[i] != 0) != right[i])
            return false;

    return true;
  }

  public static boolean equals(long [] left, boolean [] right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }

  private static boolean equals0(long [] left, Object [] right) {
    int length = right.length;
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++) {
       Object v = right[i];
       if (v instanceof Number) {
          if( ((Number)v).longValue () != left[i] )
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

  public static boolean equals(long [] left, Object [] right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }

  private static boolean equals0(long [] left, List right) {
    int length = right.size();
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++) {
       Object v = right.get(i);
       if (v instanceof Number) {
          if( ((Number)v).longValue () != left[i] )
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

  public static boolean equals(long [] left, List right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }


    public static boolean equals(float [] left, Object right) {
        if (left == right) return true;
        if (left == null || right == null) return false;

        if(right instanceof byte []) {
          return equals0(left, (byte [])right);
        } else

        if(right instanceof short []) {
          return equals0(left, (short [])right);
        } else

        if(right instanceof int []) {
          return equals0(left, (int [])right);
        } else

        if(right instanceof long []) {
          return equals0(left, (long [])right);
        } else

        if(right instanceof float []) {
          return equals0(left, (float [])right);
        } else

        if(right instanceof double []) {
          return equals0(left, (double [])right);
        } else

        if(right instanceof char []) {
          return equals0(left, (char [])right);
        } else

       if(right instanceof boolean[]) {
          return equals0(left, (boolean [])right);
       } else
       if(right instanceof Object[]) {
          return equals0(left, (Object [])right);
       } else


        return false;
    }

    private static boolean equals0(float [] left, byte [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(float [] left, byte [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(float [] left, short [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(float [] left, short [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(float [] left, int [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(float [] left, int [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(float [] left, long [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(float [] left, long [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(float [] left, float [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(float [] left, float [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(float [] left, double [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(float [] left, double [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(float [] left, char [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(float [] left, char [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

  private static boolean equals0(float [] left, boolean [] right) {
    int length = right.length;
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++)
        if ((left[i] != 0) != right[i])
            return false;

    return true;
  }

  public static boolean equals(float [] left, boolean [] right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }

  private static boolean equals0(float [] left, Object [] right) {
    int length = right.length;
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++) {
       Object v = right[i];
       if (v instanceof Number) {
          if( ((Number)v).floatValue () != left[i] )
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

  public static boolean equals(float [] left, Object [] right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }

  private static boolean equals0(float [] left, List right) {
    int length = right.size();
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++) {
       Object v = right.get(i);
       if (v instanceof Number) {
          if( ((Number)v).floatValue () != left[i] )
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

  public static boolean equals(float [] left, List right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }


    public static boolean equals(double [] left, Object right) {
        if (left == right) return true;
        if (left == null || right == null) return false;

        if(right instanceof byte []) {
          return equals0(left, (byte [])right);
        } else

        if(right instanceof short []) {
          return equals0(left, (short [])right);
        } else

        if(right instanceof int []) {
          return equals0(left, (int [])right);
        } else

        if(right instanceof long []) {
          return equals0(left, (long [])right);
        } else

        if(right instanceof float []) {
          return equals0(left, (float [])right);
        } else

        if(right instanceof double []) {
          return equals0(left, (double [])right);
        } else

        if(right instanceof char []) {
          return equals0(left, (char [])right);
        } else

       if(right instanceof boolean[]) {
          return equals0(left, (boolean [])right);
       } else
       if(right instanceof Object[]) {
          return equals0(left, (Object [])right);
       } else


        return false;
    }

    private static boolean equals0(double [] left, byte [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(double [] left, byte [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(double [] left, short [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(double [] left, short [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(double [] left, int [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(double [] left, int [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(double [] left, long [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(double [] left, long [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(double [] left, float [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(double [] left, float [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(double [] left, double [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(double [] left, double [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(double [] left, char [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(double [] left, char [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

  private static boolean equals0(double [] left, boolean [] right) {
    int length = right.length;
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++)
        if ((left[i] != 0) != right[i])
            return false;

    return true;
  }

  public static boolean equals(double [] left, boolean [] right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }

  private static boolean equals0(double [] left, Object [] right) {
    int length = right.length;
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++) {
       Object v = right[i];
       if (v instanceof Number) {
          if( ((Number)v).doubleValue () != left[i] )
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

  public static boolean equals(double [] left, Object [] right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }

  private static boolean equals0(double [] left, List right) {
    int length = right.size();
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++) {
       Object v = right.get(i);
       if (v instanceof Number) {
          if( ((Number)v).doubleValue () != left[i] )
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

  public static boolean equals(double [] left, List right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }


    public static boolean equals(char [] left, Object right) {
        if (left == right) return true;
        if (left == null || right == null) return false;

        if(right instanceof byte []) {
          return equals0(left, (byte [])right);
        } else

        if(right instanceof short []) {
          return equals0(left, (short [])right);
        } else

        if(right instanceof int []) {
          return equals0(left, (int [])right);
        } else

        if(right instanceof long []) {
          return equals0(left, (long [])right);
        } else

        if(right instanceof float []) {
          return equals0(left, (float [])right);
        } else

        if(right instanceof double []) {
          return equals0(left, (double [])right);
        } else

        if(right instanceof char []) {
          return equals0(left, (char [])right);
        } else

       if(right instanceof boolean[]) {
          return equals0(left, (boolean [])right);
       } else
       if(right instanceof Object[]) {
          return equals0(left, (Object [])right);
       } else


        return false;
    }

    private static boolean equals0(char [] left, byte [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(char [] left, byte [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(char [] left, short [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(char [] left, short [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(char [] left, int [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(char [] left, int [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(char [] left, long [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(char [] left, long [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(char [] left, float [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(char [] left, float [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(char [] left, double [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(char [] left, double [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

    private static boolean equals0(char [] left, char [] right) {
      int length = right.length;
      if (left.length != length)
          return false;

      for (int i=0; i<length; i++)
          if (left[i] != right[i])
              return false;

      return true;
    }

    public static boolean equals(char [] left, char [] right) {
        if (left == null)
          return  right == null;
        else
          if (right == null)
             return false;
        return equals0(left, right);
    }

  private static boolean equals0(char [] left, boolean [] right) {
    int length = right.length;
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++)
        if ((left[i] != 0) != right[i])
            return false;

    return true;
  }

  public static boolean equals(char [] left, boolean [] right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }

  private static boolean equals0(char [] left, Object [] right) {
    int length = right.length;
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++) {
       Object v = right[i];
       if (v instanceof Number) {
          if( ((Number)v).intValue () != left[i] )
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

  public static boolean equals(char [] left, Object [] right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }

  private static boolean equals0(char [] left, List right) {
    int length = right.size();
    if (left.length != length)
        return false;

    for (int i=0; i<length; i++) {
       Object v = right.get(i);
       if (v instanceof Number) {
          if( ((Number)v).intValue () != left[i] )
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

  public static boolean equals(char [] left, List right) {
      if (left == null)
        return  right == null;
      else
        if (right == null)
           return false;
      return equals0(left, right);
  }
}
