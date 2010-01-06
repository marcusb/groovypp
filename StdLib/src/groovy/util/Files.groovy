package groovy.util

import java.nio.ByteBuffer

@Typed class Files {
    /**
     * This method is used to throw useful exceptions when the eachFile* and eachDir closure methods
     * are used incorrectly.
     *
     * @param dir The directory to check
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     */
    private static void checkDir(final File dir) throws FileNotFoundException, IllegalArgumentException {
        if (!dir.exists())
            throw new FileNotFoundException(dir.getAbsolutePath())
        if (!dir.isDirectory())
            throw new IllegalArgumentException("The provided File object is not a directory: " + dir.getAbsolutePath())
    }

    /**
     * Common code for {@link #eachFile(File,Function1<File,?>)} and {@link #eachDir(File,Function1<File,?>)}
     *
     * @param self    a file object
     * @param op the operation to invoke
     * @param onlyDir if normal file should be skipped
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     */
    private static void eachFile(final File self, boolean onlyDir, final Function1<File,?> op)
            throws FileNotFoundException, IllegalArgumentException {
        checkDir(self)
        for (file in self.listFiles()) {
            if (!onlyDir || file.directory) {
                op(file)
            }
        }
    }

    /**
     * Invokes the closure for each 'child' file in this 'parent' folder/directory.
     * Both regular files and subfolders/subdirectories are processed.
     *
     * @param self    a File (that happens to be a folder/directory)
     * @param closure a closure (first parameter is the 'child' file)
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     * @see File#listFiles()
     * @see #eachDir(File, Closure)
     */
    static void eachFile(final File self, final Function1<File,?> closure) throws FileNotFoundException, IllegalArgumentException {
        eachFile(self, false, closure);
    }

    /**
     * Invokes the closure for each subdirectory in this directory,
     * ignoring regular files.
     *
     * @param self    a File (that happens to be a folder/directory)
     * @param closure a closure (first parameter is the subdirectory file)
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     * @see #eachFile(File, Function1<File,?>)
     */
    static void eachDir(final File self, final Function1<File,?> op) throws FileNotFoundException, IllegalArgumentException {
        eachFile(self, true, op)
    }

    /**
     * Common code for {@link #eachFileRecurse(File,boolean,Function1<File,?>)} and {@link #eachDirRecurse(File,Function1<File,?>)}
     *
     * @param self    a file object
     * @param closure the closure to invoke on each file
     * @param onlyDir if normal file should be skipped
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     */
    private static void eachFileRecurse(final File self, final boolean onlyDir, final Function1<File,?> closure)
            throws FileNotFoundException, IllegalArgumentException {
        checkDir(self)
        for (file in self.listFiles()) {
            if (file.directory) {
                closure(file)
                eachFileRecurse(file, onlyDir, closure);
            } else if (!onlyDir) {
                closure(file)
            }
        }
    }

    /**
     * Invokes the closure for each descendant file in this directory.
     * Sub-directories are recursively searched in a depth-first fashion.
     * Both regular files and subdirectories are passed to the closure.
     *
     * @param self    a File
     * @param closure a closure
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     */
    static void eachFileRecurse(final File self, final Function1<File,?> closure) throws FileNotFoundException, IllegalArgumentException {
        eachFileRecurse(self, false, closure)
    }

    /**
     * Invokes the closure for each descendant directory of this directory.
     * Sub-directories are recursively searched in a depth-first fashion.
     * Only subdirectories are passed to the closure; regular files are ignored.
     *
     * @param self    a directory
     * @param closure a closure
     * @throws FileNotFoundException    if the given directory does not exist
     * @throws IllegalArgumentException if the provided File object does not represent a directory
     * @see #eachFileRecurse(File,boolean,Function1<File,?>)
     */
    static void eachDirRecurse(File self, final Function1<File,?> closure) throws FileNotFoundException, IllegalArgumentException {
        eachFileRecurse(self, true, closure)
    }

    static Iterator<File> recurseFileIterator (File self) {
        checkDir(self)
        def files = self.listFiles()
        [files.iterator(),
         files.iterator().filter{f ->  f.directory}.map { f -> f.recurseFileIterator() }.flatten ()].iterator().flatten ()
    }

    static byte [] getBytes (File file) {
        def size = file.length()
        def io = new FileInputStream(file)
        try {
            def channel = io.channel
            try {
                def b = new byte[size]
                def buf = ByteBuffer.wrap(b)
                for (def k = 0; (k += channel.read(buf)) < size;) {}
                b
            }
            finally {
                channel.close ()
            }
        }
        finally {
            io.close ()
        }
    }

    static CharSequence getCharSequence (File file) {
        new BytesCharSequence(file.bytes)
    }

    private static class BytesCharSequence implements CharSequence {

        private final byte [] b
        private final int start
        private final int end

        BytesCharSequence(byte [] b) {
            this(b, 0, b.length)
        }

        BytesCharSequence(byte [] b, int start, int end) {
            this.@b = b
            this.@start = start
            this.@end = end
        }

        final int length() {
            end - start
        }

        char charAt(int index) {
            b [start + index]
        }

        CharSequence subSequence(int start, int end) {
            new BytesCharSequence(this.@start + start, this.@end + end)
        }

        String toString() {
            new String(b, start, end-start)
        }
    }
}