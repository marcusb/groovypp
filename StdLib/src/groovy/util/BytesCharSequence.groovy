package groovy.util

@Typed class BytesCharSequence implements CharSequence {

    private final byte [] b
    private final int start
    private final int end

    BytesCharSequence(byte [] b) {
        this(b, 0, b.length)
    }

    MetaClass getMetaClass () {}
    void setMetaClass (MetaClass mc) {}

    BytesCharSequence(byte [] b, int start, int end) {
        this.@b = b
        this.@start = start
        this.@end = end
    }

    final int length() {
        end - start
    }

    char charAt(int index) {
        ((int)b [start + index]) & 0xff
    }

    CharSequence subSequence(int start, int end) {
        new BytesCharSequence(this.b, this.start + start, this.start + end)
    }

    String toString() {
        new String(b, 0, start, end-start)
    }
}
