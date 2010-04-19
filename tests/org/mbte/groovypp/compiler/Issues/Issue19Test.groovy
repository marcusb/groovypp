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

package org.mbte.groovypp.compiler.Issues

public class Issue19Test extends GroovyShellTestCase {
    void testFail () {
        shell.parse """
package widefinder

import java.nio.ByteBuffer
import java.nio.channels.FileChannel


@Typed
class Start
{
    private static final int  CPU_NUM   = Runtime.runtime.availableProcessors()
    private static final File DATA_FILE = ["e:/Projects/groovy-booster/data/data-1000.log"]
    private static final int  R         = 0x0d
    private static final int  N         = 0x0a


    public static void main ( String[] args )
    {
        println ( [ "Buffer Size (Mb)", "CPU #", "Lines #", "Bytes Read", "Strings Size", "Time (sec)" ].join( '\t' ))

        for ( cpuNum in ( 1 .. 30 ))
        {
            for ( bufferSizeKb in ( 16 .. 1024 ).step( 16 ))
            {
                def t          = System.currentTimeMillis()
                def bufferSize = Math.min( DATA_FILE.size(), ( bufferSizeKb * 1024 ))
                def buffer     = ByteBuffer.allocate( bufferSize )
                def fis        = new FileInputStream( DATA_FILE )
                def channel    = fis.channel

                try
                {
                    print ( [ bufferSize, cpuNum, "" ].join( '\t' ))
                    long[] result = countLines( channel, buffer, cpuNum )
                    println ([ result[ 0 ], // Lines #
                               result[ 1 ], // Bytes Read
                               result[ 2 ], // String Size
                               (( System.currentTimeMillis() - t ) / 1000 ) ].join( '\t' ))
                }
                finally
                {
                    channel.close();
                    fis.close();
                }
            }
        }
    }


   /**
    * Reads number of lines in the channel specified
    */
    private static long[] countLines ( FileChannel channel, ByteBuffer buffer, int cpuNum )
    {
        buffer.rewind()
        long[] linesCounter   = [0]
        def   totalBytesRead = 0L
        def   totalChunkSize = 0L

        /**
         * Reading from channel until it ends
         *
         * REMAINING BUFFER
         */
        for ( int remaining = 0; ( channel.position() < channel.size()); )
        {
            def bytesRead = channel.read( buffer )
            def array     = buffer.array()
            totalBytesRead  += bytesRead
            boolean eof      = ( channel.position() == channel.size())

            assert (( bytesRead > 0 ) &&
                        (( bytesRead + remaining ) == buffer.position()) &&
                            ( buffer.position()    <= array.length ));

            /**
             * Iterating through buffer, giving each thread it's own String to analyze
             * "beginIndex" - ????????????????
             * "endIndex"   - ????????????????
             */
            def beginIndex = 0
            def chunkSize  = ( buffer.position() / cpuNum ) // Approximate size of byte[] chunk to be given to each thread
            for ( int endIndex = chunkSize; ( endIndex <= buffer.position()); endIndex += chunkSize )
            {
                /**
                 * "beginIndex" - ????????????????
                 * "endIndex"   - ????????????????
                 */

                if ((( buffer.position() - endIndex ) < chunkSize ) && ( eof ))
                {
                    /**
                     * Expanding it to the end of current input - otherwise, remaining bytes will be left in buffer
                     * and taken by no thread
                     */
                    endIndex = buffer.position()
                }
                else
                {
                    /**
                     * failed on \r\n sequence - looking where it ends
                     */
                    while (( endIndex < buffer.position()) && endOfLine( array[ endIndex ] )) { endIndex++ }

                    /**
                     * didn't fail on \r\n sequence - looking for it
                     * WHAT IF THERES NO ENOUGH LINES IN BUFFER FOR EACH THREAD?????? ---------------------------------
                     * ???????????????????????????????????
                     */
                    while ( ! endOfLine( array[ endIndex - 1 ] )) { endIndex--; assert ( endIndex > 0 ) }
                }

                assert (( endOfLine( array[ endIndex - 1 ] )) &&
                            (( endIndex == buffer.position()) || ( ! endOfLine( array[ endIndex ] ))))

                String chunk    = new String( array, beginIndex, ( endIndex - beginIndex ), "UTF-8" )
                totalChunkSize += chunk.size()

                assert ( chunk.size() == ( endIndex - beginIndex ))

                chunk.eachLine{ linesCounter[ 0 ]++ } // To work around static closure restriction:
                                                      // it has all variables "final", so we can't just "j++"
                beginIndex = endIndex
            }

            buffer.position( beginIndex )  // Moving buffer's position a little back - to the last known "endIndex"
            remaining = buffer.remaining() // Now we know how many bytes are left unread in it
            buffer.compact()               // Copying remaining bytes to the beginning of the buffer
        }

        assert ( totalBytesRead == channel.size()) // Making sure we read all file's data
        assert ( totalChunkSize == channel.size()) // Making sure we read all file's data as Strings
                                                    // (assuming each character took one byte in the input,  ASCII-like)
        [ linesCounter[ 0 ], totalBytesRead, totalChunkSize ]
    }


   /**
    * Determines if byte specified is an end-of-line character
    */
    private static boolean endOfLine( byte b )
    {
        (( b == 0x0D ) || ( b == 0x0A ))
    }
}
"""
    }
}