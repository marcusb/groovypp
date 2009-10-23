package org.mbte.groovypp.runtime;

public class ArraysMethods {
    private static int normaliseIndex(int i, int size) {
        int temp = i;
        if (i < 0) {
            i += size;
        }
        if (i < 0) {
            throw new ArrayIndexOutOfBoundsException("Negative array index [" + temp + "] too large for array size " + size);
        }
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
}
