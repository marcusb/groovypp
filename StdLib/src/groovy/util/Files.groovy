package groovy.util

import java.nio.ByteBuffer
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.newReader
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.newWriter
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.newInputStream
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.newOutputStream
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.newDataInputStream
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.newDataOutputStream
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.newPrintWriter
import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport

@Typed class Files extends DefaultGroovyMethodsSupport {
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
        def rec = files.iterator().filter {it.directory}.map {it.recurseFileIterator()}.flatten()
        files.iterator().followedBy(rec)
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

    /**
     * Create a new BufferedReader for this file and then
     * passes it into the closure, ensuring the reader is closed after the
     * closure returns.
     *
     * @param file    a file object
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    public static Object withReader(File file, Function1<Reader,?> closure) throws IOException {
        return withReader(newReader(file), closure);
    }

    /**
     * Create a new BufferedReader for this file using the specified charset and then
     * passes it into the closure, ensuring the reader is closed after the
     * closure returns.
     *
     * @param file    a file object
     * @param charset the charset for this input stream
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    public static Object withReader(File file, String charset, Function1<Reader,?> closure) throws IOException {
        return withReader(newReader(file, charset), closure);
    }

    /**
     * Creates a new OutputStream for this file and passes it into the closure.
     * This method ensures the stream is closed after the closure returns.
     *
     * @param file    a File
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #withStream(java.io.OutputStream, groovy.lang.Closure)
     */
    public static Object withOutputStream(File file, Function1<OutputStream,?> closure) throws IOException {
        return withStream(newOutputStream(file), closure);
    }

    /**
     * Create a new InputStream for this file and passes it into the closure.
     * This method ensures the stream is closed after the closure returns.
     *
     * @param file    a File
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #withStream(java.io.InputStream, groovy.lang.Closure)
     */
    public static Object withInputStream(File file, Function1<InputStream,?> closure) throws IOException {
        return withStream(newInputStream(file), closure);
    }

    /**
     * Creates a new InputStream for this URL and passes it into the closure.
     * This method ensures the stream is closed after the closure returns.
     *
     * @param url     a URL
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #withStream(java.io.InputStream, groovy.lang.Closure)
     */
    public static Object withInputStream(URL url, Function1<InputStream,?> closure) throws IOException {
        return withStream(newInputStream(url), closure);
    }

    /**
     * Create a new DataOutputStream for this file and passes it into the closure.
     * This method ensures the stream is closed after the closure returns.
     *
     * @param file    a File
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #withStream(java.io.OutputStream, groovy.lang.Closure)
     */
    public static Object withDataOutputStream(File file, Function1<DataOutputStream,?> closure) throws IOException {
        return withStream(newDataOutputStream(file), closure);
    }

    /**
     * Create a new DataInputStream for this file and passes it into the closure.
     * This method ensures the stream is closed after the closure returns.
     *
     * @param file    a File
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #withStream(java.io.InputStream, groovy.lang.Closure)
     */
    public static Object withDataInputStream(File file, Function1<DataInputStream,?> closure) throws IOException {
        return withStream(newDataInputStream(file), closure);
    }

    /**
     * Write a Byte Order Mark at the begining of the file
     *
     * @param stream    the FileOuputStream to write the BOM to
     * @param bigEndian true if UTF 16 Big Endian or false if Low Endian
     * @throws IOException if an IOException occurs.
     */
    private static void writeUtf16Bom(FileOutputStream stream, boolean bigEndian) throws IOException {
        if (bigEndian) {
            stream.write(-2);
            stream.write(-1);
        } else {
            stream.write(-1);
            stream.write(-2);
        }
    }

    /**
     * Creates a new BufferedWriter for this file, passes it to the closure, and
     * ensures the stream is flushed and closed after the closure returns.
     *
     * @param file    a File
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    public static Object withWriter(File file, Function1<Writer,?> closure) throws IOException {
        return withWriter(newWriter(file), closure);
    }

    /**
     * Creates a new BufferedWriter for this file, passes it to the closure, and
     * ensures the stream is flushed and closed after the closure returns.
     * The writer will use the given charset encoding.
     *
     * @param file    a File
     * @param charset the charset used
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    public static Object withWriter(File file, String charset, Function1<Writer,?> closure) throws IOException {
        return withWriter(newWriter(file, charset), closure);
    }

    /**
     * Create a new BufferedWriter which will append to this
     * file.  The writer is passed to the closure and will be closed before
     * this method returns.
     *
     * @param file    a File
     * @param charset the charset used
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    public static Object withWriterAppend(File file, String charset, Function1<Writer,?> closure) throws IOException {
        return withWriter(newWriter(file, charset, true), closure);
    }

    /**
     * Create a new BufferedWriter for this file in append mode.  The writer
     * is passed to the closure and is closed after the closure returns.
     *
     * @param file    a File
     * @param closure a closure
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    public static Object withWriterAppend(File file, Function1<Writer,?> closure) throws IOException {
        return withWriter(newWriter(file, true), closure);
    }

    /**
     * Create a new PrintWriter for this file which is then
     * passed it into the given closure.  This method ensures its the writer
     * is closed after the closure returns.
     *
     * @param file    a File
     * @param closure the closure to invoke with the PrintWriter
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    public static Object withPrintWriter(File file, Function1<PrintWriter,?> closure) throws IOException {
        return withWriter(newPrintWriter(file), closure);
    }

    /**
     * Create a new PrintWriter with a specified charset for
     * this file.  The writer is passed to the closure, and will be closed
     * before this method returns.
     *
     * @param file    a File
     * @param charset the charset
     * @param closure the closure to invoke with the PrintWriter
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    public static Object withPrintWriter(File file, String charset, Function1<PrintWriter,?> closure) throws IOException {
        return withWriter(newPrintWriter(file, charset), closure);
    }

    /**
     * Create a new PrintWriter with a specified charset for
     * this file.  The writer is passed to the closure, and will be closed
     * before this method returns.
     *
     * @param writer   a writer
     * @param closure the closure to invoke with the PrintWriter
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    public static Object withPrintWriter(Writer writer, Function1<PrintWriter,?> closure) throws IOException {
        return withWriter(newPrintWriter(writer), closure);
    }

    /**
     * Allows this writer to be used within the closure, ensuring that it
     * is flushed and closed before this method returns.
     *
     * @param writer  the writer which is used and then closed
     * @param closure the closure that the writer is passed into
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    public static Object withWriter(Writer writer, Function1<Writer,?> closure) throws IOException {
        try {
            Object result = closure.call(writer);

            try {
                writer.flush();
            } catch (IOException e) {
                // try to continue even in case of error
            }
            Writer temp = writer;
            writer = null;
            temp.close();
            return result;
        } finally {
            closeWithWarning(writer);
        }
    }

    /**
     * Allows this reader to be used within the closure, ensuring that it
     * is closed before this method returns.
     *
     * @param reader  the reader which is used and then closed
     * @param closure the closure that the writer is passed into
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    public static Object withReader(Reader reader, Function1<Reader,?> closure) throws IOException {
        try {
            Object result = closure.call(reader);

            Reader temp = reader;
            reader = null;
            temp.close();

            return result;
        } finally {
            closeWithWarning(reader);
        }
    }

    /**
     * Allows this input stream to be used within the closure, ensuring that it
     * is flushed and closed before this method returns.
     *
     * @param stream  the stream which is used and then closed
     * @param closure the closure that the stream is passed into
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    public static Object withStream(InputStream stream, Function1<InputStream,?> closure) throws IOException {
        try {
            Object result = closure.call(stream);

            InputStream temp = stream;
            stream = null;
            temp.close();

            return result;
        } finally {
            closeWithWarning(stream);
        }
    }

    /**
     * Helper method to create a new BufferedReader for a URL and then
     * passes it to the closure.  The reader is closed after the closure returns.
     *
     * @param url     a URL
     * @param closure the closure to invoke with the reader
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    public static Object withReader(URL url, Function1<Reader,?> closure) throws IOException {
        return withReader(url.openConnection().getInputStream(), closure);
    }

    /**
     * Helper method to create a new Reader for a URL and then
     * passes it to the closure.  The reader is closed after the closure returns.
     *
     * @param url     a URL
     * @param charset the charset used
     * @param closure the closure to invoke with the reader
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    public static Object withReader(URL url, String charset, Function1<Reader,?> closure) throws IOException {
        return withReader(url.openConnection().getInputStream(), charset, closure);
    }

    /**
     * Helper method to create a new Reader for a stream and then
     * passes it into the closure.  The reader (and this stream) is closed after
     * the closure returns.
     *
     * @see java.io.InputStreamReader
     * @param in      a stream
     * @param closure the closure to invoke with the InputStream
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    public static Object withReader(InputStream ins, Function1<Reader,?> closure) throws IOException {
        return withReader(new InputStreamReader(ins), closure);
    }

    /**
     * Helper method to create a new Reader for a stream and then
     * passes it into the closure.  The reader (and this stream) is closed after
     * the closure returns.
     *
     * @see java.io.InputStreamReader
     * @param in      a stream
     * @param charset the charset used to decode the stream
     * @param closure the closure to invoke with the reader
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    public static Object withReader(InputStream ins, String charset, Function1<Reader,?> closure) throws IOException {
        return withReader(new InputStreamReader(ins, charset), closure);
    }

    /**
     * Creates a writer from this stream, passing it to the given closure.
     * This method ensures the stream is closed after the closure returns.
     *
     * @param stream  the stream which is used and then closed
     * @param closure the closure that the writer is passed into
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #withWriter(java.io.Writer, groovy.lang.Function1)
     * @since 1.5.2
     */
    public static Object withWriter(OutputStream stream, Function1<Writer,?> closure) throws IOException {
        return withWriter(new OutputStreamWriter(stream), closure);
    }

    /**
     * Creates a writer from this stream, passing it to the given closure.
     * This method ensures the stream is closed after the closure returns.
     *
     * @param stream  the stream which is used and then closed
     * @param charset the charset used
     * @param closure the closure that the writer is passed into
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     * @see #withWriter(java.io.Writer, groovy.lang.Function1)
     */
    public static Object withWriter(OutputStream stream, String charset, Function1<Writer,?> closure) throws IOException {
        return withWriter(new OutputStreamWriter(stream, charset), closure);
    }

    /**
     * Passes this OutputStream to the closure, ensuring that the stream
     * is closed after the closure returns, regardless of errors.
     *
     * @param os      the stream which is used and then closed
     * @param closure the closure that the stream is passed into
     * @return the value returned by the closure
     * @throws IOException if an IOException occurs.
     */
    public static Object withStream(OutputStream os, Function1<OutputStream,?> closure) throws IOException {
        try {
            Object result = closure.call(os);
            os.flush();

            OutputStream temp = os;
            os = null;
            temp.close();

            return result;
        } finally {
            closeWithWarning(os);
        }
    }
}